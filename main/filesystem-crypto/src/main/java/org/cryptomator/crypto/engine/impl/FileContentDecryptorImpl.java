/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import static org.cryptomator.crypto.engine.impl.Constants.CHUNK_SIZE;
import static org.cryptomator.crypto.engine.impl.Constants.MAC_SIZE;
import static org.cryptomator.crypto.engine.impl.Constants.NONCE_SIZE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

import org.cryptomator.crypto.engine.AuthenticationFailedException;
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.io.ByteBuffers;

class FileContentDecryptorImpl implements FileContentDecryptor {

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
	private static final int READ_AHEAD = 2;
	private static final ExecutorService SHARED_DECRYPTION_EXECUTOR = Executors.newFixedThreadPool(NUM_THREADS);

	private final FifoParallelDataProcessor<ByteBuffer> dataProcessor = new FifoParallelDataProcessor<>(NUM_THREADS + READ_AHEAD, SHARED_DECRYPTION_EXECUTOR);
	private final Supplier<Mac> hmacSha256;
	private final FileHeader header;
	private final boolean authenticate;
	private final LongAdder cleartextBytesDecrypted = new LongAdder();
	private ByteBuffer ciphertextBuffer = ByteBuffer.allocate(CHUNK_SIZE);
	private long chunkNumber = 0;

	public FileContentDecryptorImpl(SecretKey headerKey, SecretKey macKey, ByteBuffer header, long firstCiphertextByte, boolean authenticate) {
		this.hmacSha256 = new ThreadLocalMac(macKey, HMAC_SHA256);
		this.header = FileHeader.decrypt(headerKey, hmacSha256, header);
		this.authenticate = authenticate;
		this.chunkNumber = firstCiphertextByte / CHUNK_SIZE; // floor() by int-truncation
	}

	@Override
	public long contentLength() {
		return header.getPayload().getFilesize();
	}

	@Override
	public void append(ByteBuffer ciphertext) throws InterruptedException {
		if (ciphertext == FileContentCryptor.EOF) {
			submitCiphertextBuffer();
			submitEof();
		} else {
			while (ciphertext.hasRemaining()) {
				ByteBuffers.copy(ciphertext, ciphertextBuffer);
				submitCiphertextBufferIfFull();
			}
		}
	}

	@Override
	public void cancelWithException(Exception cause) throws InterruptedException {
		dataProcessor.submit(() -> {
			throw cause;
		});
	}

	private void submitCiphertextBufferIfFull() throws InterruptedException {
		if (!ciphertextBuffer.hasRemaining()) {
			submitCiphertextBuffer();
			ciphertextBuffer = ByteBuffer.allocate(CHUNK_SIZE);
		}
	}

	private void submitCiphertextBuffer() throws InterruptedException {
		ciphertextBuffer.flip();
		if (ciphertextBuffer.hasRemaining()) {
			Callable<ByteBuffer> encryptionJob = new DecryptionJob(ciphertextBuffer, chunkNumber++);
			dataProcessor.submit(encryptionJob);
		}
	}

	private void submitEof() throws InterruptedException {
		dataProcessor.submitPreprocessed(FileContentCryptor.EOF);
	}

	@Override
	public ByteBuffer cleartext() throws InterruptedException {
		try {
			final ByteBuffer cleartext = dataProcessor.processedData();
			long bytesUntilLogicalEof = contentLength() - cleartextBytesDecrypted.sum();
			if (bytesUntilLogicalEof <= 0) {
				return FileContentCryptor.EOF;
			} else if (bytesUntilLogicalEof < cleartext.remaining()) {
				cleartext.limit((int) bytesUntilLogicalEof);
			}
			cleartextBytesDecrypted.add(cleartext.remaining());
			return cleartext;
		} catch (ExecutionException e) {
			if (e.getCause() instanceof AuthenticationFailedException) {
				throw new AuthenticationFailedException(e);
			} else if (e.getCause() instanceof IOException || e.getCause() instanceof UncheckedIOException) {
				throw new UncheckedIOException(new IOException("Decryption failed due to I/O exception during ciphertext supply.", e));
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void destroy() {
		header.destroy();
	}

	private class DecryptionJob implements Callable<ByteBuffer> {

		private final byte[] nonce;
		private final ByteBuffer inBuf;
		private final ByteBuffer chunkNumberBigEndian = ByteBuffer.allocate(Long.BYTES);
		private final byte[] expectedMac;

		public DecryptionJob(ByteBuffer ciphertextChunk, long chunkNumber) {
			if (ciphertextChunk.remaining() < NONCE_SIZE + MAC_SIZE) {
				throw new IllegalArgumentException("Chunk must at least contain a NONCE and a MAC");
			}
			this.nonce = new byte[NONCE_SIZE];
			ByteBuffer nonceBuf = ciphertextChunk.asReadOnlyBuffer();
			nonceBuf.position(0).limit(NONCE_SIZE);
			nonceBuf.get(nonce);
			this.inBuf = ciphertextChunk.asReadOnlyBuffer();
			this.inBuf.position(NONCE_SIZE).limit(ciphertextChunk.limit() - MAC_SIZE);
			chunkNumberBigEndian.putLong(chunkNumber);
			chunkNumberBigEndian.rewind();
			this.expectedMac = new byte[MAC_SIZE];
			ByteBuffer macBuf = ciphertextChunk.asReadOnlyBuffer();
			macBuf.position(macBuf.limit() - MAC_SIZE);
			macBuf.get(expectedMac);
		}

		@Override
		public ByteBuffer call() {
			try {
				if (authenticate) {
					Mac mac = hmacSha256.get();
					mac.update(header.getIv());
					mac.update(chunkNumberBigEndian.asReadOnlyBuffer());
					mac.update(nonce);
					mac.update(inBuf.asReadOnlyBuffer());
					if (!MessageDigest.isEqual(expectedMac, mac.doFinal())) {
						chunkNumberBigEndian.rewind();
						throw new AuthenticationFailedException("Auth error in chunk " + chunkNumberBigEndian.getLong());
					}
				}

				Cipher cipher = ThreadLocalAesCtrCipher.get();
				cipher.init(Cipher.DECRYPT_MODE, header.getPayload().getContentKey(), new IvParameterSpec(nonce));
				ByteBuffer outBuf = ByteBuffer.allocate(cipher.getOutputSize(inBuf.remaining()));
				cipher.update(inBuf, outBuf);
				outBuf.flip();
				return outBuf;
			} catch (InvalidKeyException e) {
				throw new IllegalStateException("File content key created by current class invalid.", e);
			} catch (ShortBufferException e) {
				throw new IllegalStateException("Buffer allocated for reported output size apparently not big enought.", e);
			} catch (InvalidAlgorithmParameterException e) {
				throw new IllegalStateException("CTR mode known to accept an IV (aka. nonce).", e);
			}
		}

	}

}

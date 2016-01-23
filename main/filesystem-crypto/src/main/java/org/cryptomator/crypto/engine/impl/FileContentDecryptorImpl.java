/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import static org.cryptomator.crypto.engine.impl.FileContentCryptorImpl.CHUNK_SIZE;
import static org.cryptomator.crypto.engine.impl.FileContentCryptorImpl.MAC_SIZE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

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

	private static final int AES_BLOCK_LENGTH_IN_BYTES = 16;
	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
	private static final int READ_AHEAD = 2;

	private final FifoParallelDataProcessor<ByteBuffer> dataProcessor = new FifoParallelDataProcessor<>(NUM_THREADS, NUM_THREADS + READ_AHEAD);
	private final ThreadLocal<Mac> hmacSha256;
	private final FileHeader header;
	private final boolean authenticate;
	private ByteBuffer ciphertextBuffer = ByteBuffer.allocate(CHUNK_SIZE + MAC_SIZE);
	private long chunkNumber = 0;

	public FileContentDecryptorImpl(SecretKey headerKey, SecretKey macKey, ByteBuffer header, long firstCiphertextByte, boolean authenticate) {
		final ThreadLocalMac hmacSha256 = new ThreadLocalMac(macKey, HMAC_SHA256);
		this.hmacSha256 = hmacSha256;
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
			ciphertextBuffer = ByteBuffer.allocate(CHUNK_SIZE + MAC_SIZE);
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
			return dataProcessor.processedData();
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

		private final ByteBuffer ciphertextChunk;
		private final byte[] expectedMac;
		private final byte[] nonceAndCtr;

		public DecryptionJob(ByteBuffer ciphertextChunk, long chunkNumber) {
			if (ciphertextChunk.remaining() < MAC_SIZE) {
				throw new IllegalArgumentException("Chunk must end with a MAC");
			}
			this.ciphertextChunk = ciphertextChunk.asReadOnlyBuffer();
			this.ciphertextChunk.position(0).limit(ciphertextChunk.limit() - MAC_SIZE);
			this.expectedMac = new byte[MAC_SIZE];
			ByteBuffer macBuf = ciphertextChunk.asReadOnlyBuffer();
			macBuf.position(macBuf.limit() - MAC_SIZE);
			macBuf.get(expectedMac);

			final ByteBuffer nonceAndCounterBuf = ByteBuffer.allocate(AES_BLOCK_LENGTH_IN_BYTES);
			nonceAndCounterBuf.put(header.getNonce());
			nonceAndCounterBuf.putLong(chunkNumber * CHUNK_SIZE / AES_BLOCK_LENGTH_IN_BYTES);
			this.nonceAndCtr = nonceAndCounterBuf.array();
		}

		@Override
		public ByteBuffer call() {
			try {
				if (authenticate) {
					Mac mac = hmacSha256.get();
					mac.update(ciphertextChunk.asReadOnlyBuffer());
					if (!MessageDigest.isEqual(expectedMac, mac.doFinal())) {
						throw new AuthenticationFailedException();
					}
				}

				Cipher cipher = ThreadLocalAesCtrCipher.get();
				cipher.init(Cipher.DECRYPT_MODE, header.getPayload().getContentKey(), new IvParameterSpec(nonceAndCtr));
				ByteBuffer cleartextChunk = ByteBuffer.allocate(cipher.getOutputSize(ciphertextChunk.remaining()));
				cipher.update(ciphertextChunk, cleartextChunk);
				cleartextChunk.flip();
				return cleartextChunk;
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

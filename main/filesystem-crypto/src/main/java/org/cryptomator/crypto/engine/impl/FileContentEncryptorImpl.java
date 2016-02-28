/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import static org.cryptomator.crypto.engine.impl.Constants.NONCE_SIZE;
import static org.cryptomator.crypto.engine.impl.Constants.PAYLOAD_SIZE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;
import org.cryptomator.io.ByteBuffers;

class FileContentEncryptorImpl implements FileContentEncryptor {

	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final int PADDING_LOWER_BOUND = 4 * 1024; // 4k
	private static final int PADDING_UPPER_BOUND = 16 * 1024 * 1024; // 16M
	private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
	private static final int READ_AHEAD = 2;
	private static final ExecutorService SHARED_DECRYPTION_EXECUTOR = Executors.newFixedThreadPool(NUM_THREADS);

	private final FifoParallelDataProcessor<ByteBuffer> dataProcessor = new FifoParallelDataProcessor<>(NUM_THREADS + READ_AHEAD, SHARED_DECRYPTION_EXECUTOR);
	private final ThreadLocalMac hmacSha256;
	private final SecretKey headerKey;
	private final FileHeader header;
	private final SecureRandom randomSource;
	private final LongAdder cleartextBytesScheduledForEncryption = new LongAdder();
	private ByteBuffer cleartextBuffer = ByteBuffer.allocate(PAYLOAD_SIZE);
	private long chunkNumber = 0;

	public FileContentEncryptorImpl(SecretKey headerKey, SecretKey macKey, SecureRandom randomSource, long firstCleartextByte) {
		if (firstCleartextByte != 0) {
			throw new UnsupportedOperationException("Partial encryption not supported.");
		}
		this.hmacSha256 = new ThreadLocalMac(macKey, HMAC_SHA256);
		this.headerKey = headerKey;
		this.header = new FileHeader(randomSource);
		this.randomSource = randomSource;
	}

	@Override
	public ByteBuffer getHeader() {
		header.getPayload().setFilesize(cleartextBytesScheduledForEncryption.sum());
		return header.toByteBuffer(headerKey, hmacSha256);
	}

	@Override
	public int getHeaderSize() {
		return FileHeader.HEADER_SIZE;
	}

	@Override
	public void append(ByteBuffer cleartext) throws InterruptedException {
		cleartextBytesScheduledForEncryption.add(cleartext.remaining());
		if (cleartext == FileContentCryptor.EOF) {
			appendSizeObfuscationPadding(cleartextBytesScheduledForEncryption.sum());
			submitCleartextBuffer();
			submitEof();
		} else {
			appendAllAndSubmitIfFull(cleartext);
		}
	}

	private void appendSizeObfuscationPadding(long actualSize) throws InterruptedException {
		final int maxPaddingLength = (int) Math.min(Math.max(actualSize / 10, PADDING_LOWER_BOUND), PADDING_UPPER_BOUND); // preferably 10%, but at least lower bound and no more than upper bound
		final int randomPaddingLength = randomSource.nextInt(maxPaddingLength);
		final ByteBuffer buf = ByteBuffer.allocate(PAYLOAD_SIZE);
		int remainingPadding = randomPaddingLength;
		while (remainingPadding > 0) {
			int bytesInCurrentIteration = Math.min(remainingPadding, PAYLOAD_SIZE);
			buf.clear().limit(bytesInCurrentIteration);
			appendAllAndSubmitIfFull(buf);
			remainingPadding -= bytesInCurrentIteration;
		}
	}

	private void appendAllAndSubmitIfFull(ByteBuffer cleartext) throws InterruptedException {
		while (cleartext.hasRemaining()) {
			ByteBuffers.copy(cleartext, cleartextBuffer);
			submitCleartextBufferIfFull();
		}
	}

	@Override
	public void cancelWithException(Exception cause) throws InterruptedException {
		dataProcessor.submit(() -> {
			throw cause;
		});
	}

	private void submitCleartextBufferIfFull() throws InterruptedException {
		if (!cleartextBuffer.hasRemaining()) {
			submitCleartextBuffer();
			cleartextBuffer = ByteBuffer.allocate(PAYLOAD_SIZE);
		}
	}

	private void submitCleartextBuffer() throws InterruptedException {
		cleartextBuffer.flip();
		if (cleartextBuffer.hasRemaining()) {
			Callable<ByteBuffer> encryptionJob = new EncryptionJob(cleartextBuffer, chunkNumber++);
			dataProcessor.submit(encryptionJob);
		}
	}

	private void submitEof() throws InterruptedException {
		dataProcessor.submitPreprocessed(FileContentCryptor.EOF);
	}

	@Override
	public ByteBuffer ciphertext() throws InterruptedException {
		try {
			return dataProcessor.processedData();
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException || e.getCause() instanceof UncheckedIOException) {
				throw new UncheckedIOException(new IOException("Encryption failed due to I/O exception during cleartext supply.", e));
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void destroy() {
		header.destroy();
	}

	private class EncryptionJob implements Callable<ByteBuffer> {

		private final ByteBuffer inBuf;
		private final ByteBuffer chunkNumberBigEndian = ByteBuffer.allocate(Long.BYTES);

		public EncryptionJob(ByteBuffer cleartextChunk, long chunkNumber) {
			this.inBuf = cleartextChunk;
			chunkNumberBigEndian.putLong(chunkNumber);
			chunkNumberBigEndian.rewind();
		}

		@Override
		public ByteBuffer call() {
			try {
				final Cipher cipher = ThreadLocalAesCtrCipher.get();
				final Mac mac = hmacSha256.get();
				final ByteBuffer outBuf = ByteBuffer.allocate(NONCE_SIZE + inBuf.remaining() + mac.getMacLength());

				// nonce
				byte[] nonce = new byte[NONCE_SIZE];
				randomSource.nextBytes(nonce);
				outBuf.put(nonce);

				// payload:
				cipher.init(Cipher.ENCRYPT_MODE, header.getPayload().getContentKey(), new IvParameterSpec(nonce));
				assert cipher.getOutputSize(inBuf.remaining()) == inBuf.remaining() : "input length should be equal to output length in CTR mode.";
				int bytesEncrypted = cipher.update(inBuf, outBuf);

				// mac:
				ByteBuffer ciphertextBuf = outBuf.asReadOnlyBuffer();
				ciphertextBuf.position(NONCE_SIZE).limit(NONCE_SIZE + bytesEncrypted);
				mac.update(header.getIv());
				mac.update(chunkNumberBigEndian.asReadOnlyBuffer());
				mac.update(nonce);
				mac.update(ciphertextBuf);
				byte[] authenticationCode = mac.doFinal();
				outBuf.put(authenticationCode);

				// flip and return:
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

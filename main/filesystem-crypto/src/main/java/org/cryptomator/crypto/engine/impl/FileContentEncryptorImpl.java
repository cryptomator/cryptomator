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

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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

	private static final int AES_BLOCK_LENGTH_IN_BYTES = 16;
	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
	private static final int READ_AHEAD = 2;

	private final FifoParallelDataProcessor<ByteBuffer> dataProcessor = new FifoParallelDataProcessor<>(NUM_THREADS, NUM_THREADS + READ_AHEAD);
	private final ThreadLocalMac hmacSha256;
	private final FileHeader header;
	private final SecretKey headerKey;
	private final LongAdder cleartextBytesEncrypted = new LongAdder();
	private ByteBuffer cleartextBuffer = ByteBuffer.allocate(CHUNK_SIZE);
	private long chunkNumber = 0;

	public FileContentEncryptorImpl(SecretKey headerKey, SecretKey macKey, SecureRandom randomSource, long firstCleartextByte) {
		if (firstCleartextByte != 0) {
			throw new UnsupportedOperationException("Partial encryption not supported.");
		}
		this.hmacSha256 = new ThreadLocalMac(macKey, HMAC_SHA256);
		this.headerKey = headerKey;
		this.header = new FileHeader(randomSource);
	}

	@Override
	public ByteBuffer getHeader() {
		header.getPayload().setFilesize(cleartextBytesEncrypted.sum());
		return header.toByteBuffer(headerKey, hmacSha256);
	}

	@Override
	public void append(ByteBuffer cleartext) throws InterruptedException {
		cleartextBytesEncrypted.add(cleartext.remaining());
		if (cleartext == FileContentCryptor.EOF) {
			submitCleartextBuffer();
			submitEof();
		} else {
			while (cleartext.hasRemaining()) {
				ByteBuffers.copy(cleartext, cleartextBuffer);
				submitCleartextBufferIfFull();
			}
		}
	}

	private void submitCleartextBufferIfFull() throws InterruptedException {
		if (!cleartextBuffer.hasRemaining()) {
			submitCleartextBuffer();
			cleartextBuffer = ByteBuffer.allocate(CHUNK_SIZE);
		}
	}

	private void submitCleartextBuffer() throws InterruptedException {
		cleartextBuffer.flip();
		Callable<ByteBuffer> encryptionJob = new EncryptionJob(cleartextBuffer, chunkNumber++);
		dataProcessor.submit(encryptionJob);
	}

	private void submitEof() throws InterruptedException {
		dataProcessor.submitPreprocessed(FileContentCryptor.EOF);
	}

	@Override
	public ByteBuffer ciphertext() throws InterruptedException {
		try {
			return dataProcessor.processedData();
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void destroy() {
		header.destroy();
	}

	private class EncryptionJob implements Callable<ByteBuffer> {

		private final ByteBuffer cleartextChunk;
		private final byte[] nonceAndCtr;

		public EncryptionJob(ByteBuffer cleartextChunk, long chunkNumber) {
			this.cleartextChunk = cleartextChunk;

			final ByteBuffer nonceAndCounterBuf = ByteBuffer.allocate(AES_BLOCK_LENGTH_IN_BYTES);
			nonceAndCounterBuf.put(header.getNonce());
			nonceAndCounterBuf.putLong(chunkNumber * CHUNK_SIZE / AES_BLOCK_LENGTH_IN_BYTES);
			this.nonceAndCtr = nonceAndCounterBuf.array();
		}

		@Override
		public ByteBuffer call() {
			try {
				Cipher cipher = ThreadLocalAesCtrCipher.get();
				cipher.init(Cipher.ENCRYPT_MODE, header.getPayload().getContentKey(), new IvParameterSpec(nonceAndCtr));
				Mac mac = hmacSha256.get();
				ByteBuffer ciphertextChunk = ByteBuffer.allocate(cipher.getOutputSize(cleartextChunk.remaining()) + mac.getMacLength());
				cipher.update(cleartextChunk, ciphertextChunk);
				ByteBuffer ciphertextSoFar = ciphertextChunk.asReadOnlyBuffer();
				ciphertextSoFar.flip();
				mac.update(ciphertextSoFar);
				byte[] authenticationCode = mac.doFinal();
				ciphertextChunk.put(authenticationCode);
				ciphertextChunk.flip();
				return ciphertextChunk;
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

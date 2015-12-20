package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import org.cryptomator.crypto.engine.ByteRange;
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.io.ByteBuffers;

class FileContentDecryptorImpl implements FileContentDecryptor {

	private static final String AES = "AES";
	private static final int AES_BLOCK_LENGTH_IN_BYTES = 16;
	private static final String AES_CBC = "AES/CBC/PKCS5Padding";
	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final int CHUNK_SIZE = 32 * 1024;
	private static final int MAC_SIZE = 32;
	private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
	private static final int READ_AHEAD = 2;

	private final FifoParallelDataProcessor<ByteBuffer> dataProcessor = new FifoParallelDataProcessor<>(NUM_THREADS, NUM_THREADS + READ_AHEAD);
	private final ThreadLocal<Mac> hmacSha256;
	private final SecretKey contentKey;
	private final byte[] nonce;
	private final long cleartextLength;
	private ByteBuffer ciphertextBuffer = ByteBuffer.allocate(CHUNK_SIZE + MAC_SIZE);
	private long chunkNumber = 0;

	public FileContentDecryptorImpl(SecretKey headerKey, SecretKey macKey, ByteBuffer header) {
		this.hmacSha256 = new ThreadLocalMac(macKey, HMAC_SHA256);

		checkHeaderMac(header, hmacSha256.get());

		this.nonce = new byte[8];
		ByteBuffer nonceBuffer = header.asReadOnlyBuffer();
		nonceBuffer.position(16).limit(24);
		nonceBuffer.get(this.nonce);

		byte[] contentKeyBytes = new byte[32];
		ByteBuffer sensitiveDataBuffer = getCleartextSensitiveHeaderData(header, headerKey);
		this.cleartextLength = sensitiveDataBuffer.getLong();
		sensitiveDataBuffer.get(contentKeyBytes);
		this.contentKey = new SecretKeySpec(contentKeyBytes, AES);

	}

	private static void checkHeaderMac(ByteBuffer header, Mac mac) throws IllegalArgumentException {
		assert mac.getMacLength() == MAC_SIZE;
		ByteBuffer headerData = header.asReadOnlyBuffer();
		headerData.position(0).limit(72);
		mac.update(headerData);
		ByteBuffer headerMac = header.asReadOnlyBuffer();
		headerMac.position(72).limit(72 + MAC_SIZE);
		byte[] expectedMac = new byte[MAC_SIZE];
		headerMac.get(expectedMac);

		if (!MessageDigest.isEqual(expectedMac, mac.doFinal())) {
			throw new IllegalArgumentException("Corrupt header.");
		}
	}

	private static ByteBuffer getCleartextSensitiveHeaderData(ByteBuffer header, SecretKey headerKey) {
		try {
			byte[] iv = new byte[16];
			ByteBuffer ivBuffer = header.asReadOnlyBuffer();
			ivBuffer.position(0).limit(16);
			ivBuffer.get(iv);

			ByteBuffer sensitiveHeaderDataBuffer = header.asReadOnlyBuffer();
			sensitiveHeaderDataBuffer.position(24).limit(72);

			final Cipher cipher = Cipher.getInstance(AES_CBC);
			cipher.init(Cipher.DECRYPT_MODE, headerKey, new IvParameterSpec(iv));
			final int cleartextLength = cipher.getOutputSize(sensitiveHeaderDataBuffer.remaining());
			assert cleartextLength == 48 : "decryption shouldn't need more output than input buffer size.";
			final ByteBuffer cleartext = ByteBuffer.allocate(cleartextLength);
			cipher.doFinal(sensitiveHeaderDataBuffer, cleartext);
			cleartext.flip();
			return cleartext;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException("Unable to decrypt header.", e);
		}
	}

	@Override
	public long contentLength() {
		return cleartextLength;
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

	private void submitCiphertextBufferIfFull() throws InterruptedException {
		if (!ciphertextBuffer.hasRemaining()) {
			submitCiphertextBuffer();
			ciphertextBuffer = ByteBuffer.allocate(CHUNK_SIZE + MAC_SIZE);
		}
	}

	private void submitCiphertextBuffer() throws InterruptedException {
		ciphertextBuffer.flip();
		Callable<ByteBuffer> encryptionJob = new DecryptionJob(ciphertextBuffer, chunkNumber++);
		dataProcessor.submit(encryptionJob);
	}

	private void submitEof() throws InterruptedException {
		dataProcessor.submitPreprocessed(FileContentCryptor.EOF);
	}

	@Override
	public ByteBuffer cleartext() throws InterruptedException {
		return dataProcessor.processedData();
	}

	@Override
	public ByteRange ciphertextRequiredToDecryptRange(ByteRange cleartextRange) {
		return ByteRange.of(0, Long.MAX_VALUE);
	}

	@Override
	public void skipToPosition(long nextCiphertextByte) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Partial decryption not supported.");
	}

	@Override
	public void destroy() {
		try {
			contentKey.destroy();
		} catch (DestroyFailedException e) {
			// ignore
		}
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
			nonceAndCounterBuf.put(nonce);
			nonceAndCounterBuf.putLong(chunkNumber * CHUNK_SIZE / AES_BLOCK_LENGTH_IN_BYTES);
			this.nonceAndCtr = nonceAndCounterBuf.array();
		}

		@Override
		public ByteBuffer call() {
			try {
				Mac mac = hmacSha256.get();
				mac.update(ciphertextChunk.asReadOnlyBuffer());
				if (!MessageDigest.isEqual(expectedMac, mac.doFinal())) {
					// TODO handle invalid MAC properly
					throw new IllegalArgumentException("Corrupt mac.");
				}

				Cipher cipher = ThreadLocalAesCtrCipher.get();
				cipher.init(Cipher.DECRYPT_MODE, contentKey, new IvParameterSpec(nonceAndCtr));
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

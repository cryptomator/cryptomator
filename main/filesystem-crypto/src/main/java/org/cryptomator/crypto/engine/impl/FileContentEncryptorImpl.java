package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.LongAdder;

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
import org.cryptomator.crypto.engine.FileContentEncryptor;
import org.cryptomator.io.ByteBuffers;

class FileContentEncryptorImpl extends AbstractFileContentProcessor implements FileContentEncryptor {

	private static final String AES = "AES";
	private static final int AES_BLOCK_LENGTH_IN_BYTES = 16;
	private static final String AES_CBC = "AES/CBC/PKCS5Padding";
	private static final String HMAC_SHA256 = "HmacSHA256";
	private static final int CHUNK_SIZE = 32 * 1024;

	private final ThreadLocal<Mac> hmacSha256;
	private final SecretKey headerKey;
	private final SecretKey contentKey;
	private final byte[] iv;
	private final byte[] nonce;
	private final LongAdder cleartextBytesEncrypted = new LongAdder();
	private ByteBuffer cleartextBuffer = ByteBuffer.allocate(CHUNK_SIZE);
	private long chunkNumber = 0;

	public FileContentEncryptorImpl(SecretKey headerKey, SecretKey macKey, SecureRandom randomSource) {
		this.hmacSha256 = new ThreadLocalMac(macKey, HMAC_SHA256);
		this.headerKey = headerKey;
		this.iv = new byte[16];
		this.nonce = new byte[8];
		final byte[] contentKeyBytes = new byte[32];
		randomSource.nextBytes(iv);
		randomSource.nextBytes(nonce);
		randomSource.nextBytes(contentKeyBytes);
		this.contentKey = new SecretKeySpec(contentKeyBytes, AES);
	}

	private ByteBuffer getCleartextSensitiveHeaderData() {
		ByteBuffer header = ByteBuffer.allocate(104);
		header.putLong(cleartextBytesEncrypted.sum());
		header.put(contentKey.getEncoded());
		header.flip();
		return header;
	}

	private ByteBuffer getCiphertextSensitiveHeaderData() {
		final ByteBuffer cleartext = getCleartextSensitiveHeaderData();
		try {
			final Cipher cipher = Cipher.getInstance(AES_CBC);
			cipher.init(Cipher.ENCRYPT_MODE, headerKey, new IvParameterSpec(iv));
			final int ciphertextLength = cipher.getOutputSize(cleartext.remaining());
			assert ciphertextLength == 48 : "8 byte long and 32 byte file key should fit into 3 blocks";
			final ByteBuffer ciphertext = ByteBuffer.allocate(ciphertextLength);
			cipher.doFinal(cleartext, ciphertext);
			ciphertext.flip();
			return ciphertext;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException("Unable to compute encrypted header.", e);
		}
	}

	private byte[] mac(ByteBuffer what) {
		Mac mac = hmacSha256.get();
		mac.update(what);
		return mac.doFinal();
	}

	@Override
	public ByteBuffer getHeader() {
		final ByteBuffer header = ByteBuffer.allocate(FileContentCryptorImpl.HEADER_SIZE);
		header.put(iv);
		header.put(nonce);
		final ByteBuffer sensitiveHeaderData = getCiphertextSensitiveHeaderData();
		header.put(sensitiveHeaderData);
		final ByteBuffer headerSoFar = header.asReadOnlyBuffer();
		headerSoFar.flip();
		header.put(mac(headerSoFar));
		header.flip();
		return header;
	}

	@Override
	public void append(ByteBuffer cleartext) {
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

	private void submitCleartextBufferIfFull() {
		if (!cleartextBuffer.hasRemaining()) {
			submitCleartextBuffer();
			cleartextBuffer = ByteBuffer.allocate(CHUNK_SIZE);
		}
	}

	private void submitCleartextBuffer() {
		cleartextBuffer.flip();
		Callable<ByteBuffer> encryptionJob = new EncryptionJob(cleartextBuffer, chunkNumber++);
		submit(encryptionJob);
	}

	private void submitEof() {
		submitPreprocessed(FileContentCryptor.EOF);
	}

	@Override
	public ByteBuffer ciphertext() throws InterruptedException {
		return processedData();
	}

	@Override
	public ByteRange cleartextRequiredToEncryptRange(ByteRange cleartextRange) {
		return ByteRange.of(0, Long.MAX_VALUE);
	}

	@Override
	public void skipToPosition(long nextCleartextByte) throws IllegalArgumentException {
		throw new UnsupportedOperationException("Partial encryption not supported.");
	}

	@Override
	public void destroy() {
		try {
			contentKey.destroy();
		} catch (DestroyFailedException e) {
			// ignore
		}
	}

	@Override
	public void close() {
		this.destroy();
		super.close();
	}

	private class EncryptionJob implements Callable<ByteBuffer> {

		private final ByteBuffer cleartextChunk;
		private final byte[] nonceAndCtr;

		public EncryptionJob(ByteBuffer cleartextChunk, long chunkNumber) {
			this.cleartextChunk = cleartextChunk;

			final ByteBuffer nonceAndCounterBuf = ByteBuffer.allocate(AES_BLOCK_LENGTH_IN_BYTES);
			nonceAndCounterBuf.put(nonce);
			nonceAndCounterBuf.putLong(chunkNumber * CHUNK_SIZE / AES_BLOCK_LENGTH_IN_BYTES);
			this.nonceAndCtr = nonceAndCounterBuf.array();
		}

		@Override
		public ByteBuffer call() {
			try {
				Cipher cipher = ThreadLocalAesCtrCipher.get();
				cipher.init(Cipher.ENCRYPT_MODE, contentKey, new IvParameterSpec(nonceAndCtr));
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

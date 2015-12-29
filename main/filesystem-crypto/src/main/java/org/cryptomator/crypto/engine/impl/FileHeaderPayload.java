/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

class FileHeaderPayload implements Destroyable {

	private static final int FILESIZE_POS = 0;
	private static final int FILESIZE_LEN = Long.BYTES;
	private static final int CONTENT_KEY_POS = 8;
	private static final int CONTENT_KEY_LEN = 32;
	private static final String AES = "AES";
	private static final String AES_CBC = "AES/CBC/PKCS5Padding";

	private long filesize;
	private final SecretKey contentKey;

	public FileHeaderPayload(SecureRandom randomSource) {
		filesize = 0;
		final byte[] contentKey = new byte[CONTENT_KEY_LEN];
		try {
			randomSource.nextBytes(contentKey);
			this.contentKey = new SecretKeySpec(contentKey, AES);
		} finally {
			Arrays.fill(contentKey, (byte) 0x00);
		}
	}

	private FileHeaderPayload(long filesize, SecretKey contentKey) {
		this.filesize = filesize;
		this.contentKey = contentKey;
	}

	public long getFilesize() {
		return filesize;
	}

	public void setFilesize(long filesize) {
		this.filesize = filesize;
	}

	public SecretKey getContentKey() {
		return contentKey;
	}

	@Override
	public boolean isDestroyed() {
		return contentKey.isDestroyed();
	}

	@Override
	public void destroy() {
		try {
			contentKey.destroy();
		} catch (DestroyFailedException e) {
			// no-op
		}
	}

	private ByteBuffer toCleartextByteBuffer() {
		ByteBuffer cleartext = ByteBuffer.allocate(FILESIZE_LEN + CONTENT_KEY_LEN);
		cleartext.position(FILESIZE_POS).limit(FILESIZE_POS + FILESIZE_LEN);
		cleartext.putLong(filesize);
		cleartext.position(CONTENT_KEY_POS).limit(CONTENT_KEY_POS + CONTENT_KEY_LEN);
		cleartext.put(contentKey.getEncoded());
		cleartext.flip();
		return cleartext;
	}

	public ByteBuffer toCiphertextByteBuffer(SecretKey headerKey, byte[] iv) {
		final ByteBuffer cleartext = toCleartextByteBuffer();
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
		} finally {
			Arrays.fill(cleartext.array(), (byte) 0x00);
		}
	}

	public static FileHeaderPayload fromCiphertextByteBuffer(ByteBuffer ciphertextPayload, SecretKey headerKey, byte[] iv) {
		final ByteBuffer cleartext = decryptPayload(ciphertextPayload, headerKey, iv);
		try {
			return fromCleartextByteBuffer(cleartext);
		} finally {
			// destroy evidence:
			Arrays.fill(cleartext.array(), (byte) 0x00);
		}
	}

	private static FileHeaderPayload fromCleartextByteBuffer(ByteBuffer cleartext) {
		final byte[] contentKey = new byte[CONTENT_KEY_LEN];
		try {
			cleartext.position(FILESIZE_POS).limit(FILESIZE_POS + FILESIZE_LEN);
			final long filesize = cleartext.getLong();
			cleartext.position(CONTENT_KEY_POS).limit(CONTENT_KEY_POS + CONTENT_KEY_LEN);
			cleartext.get(contentKey);
			return new FileHeaderPayload(filesize, new SecretKeySpec(contentKey, AES));
		} finally {
			// destroy evidence:
			Arrays.fill(contentKey, (byte) 0x00);
		}
	}

	private static ByteBuffer decryptPayload(ByteBuffer ciphertext, SecretKey headerKey, byte[] iv) {
		try {
			final Cipher cipher = Cipher.getInstance(AES_CBC);
			cipher.init(Cipher.DECRYPT_MODE, headerKey, new IvParameterSpec(iv));
			final int cleartextLength = cipher.getOutputSize(ciphertext.remaining());
			assert cleartextLength == ciphertext.remaining() : "decryption shouldn't need more output than input buffer size.";
			final ByteBuffer cleartext = ByteBuffer.allocate(cleartextLength);
			cipher.doFinal(ciphertext, cleartext);
			cleartext.flip();
			return cleartext;
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException("Unable to decrypt header.", e);
		}
	}

}

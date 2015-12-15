/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.BaseNCodec;
import org.cryptomator.crypto.engine.CryptoException;
import org.cryptomator.crypto.engine.FilenameCryptor;
import org.cryptomator.siv.SivMode;

class FilenameCryptorImpl implements FilenameCryptor {

	private static final BaseNCodec BASE32 = new Base32();
	private static final ThreadLocal<MessageDigest> SHA256 = new ThreadLocalSha256();
	private static final SivMode AES_SIV = new SivMode();

	private final SecretKey encryptionKey;
	private final SecretKey macKey;

	FilenameCryptorImpl(SecretKey encryptionKey, SecretKey macKey) {
		if (encryptionKey == null || macKey == null) {
			throw new IllegalArgumentException("Key must not be null");
		}
		this.encryptionKey = encryptionKey;
		this.macKey = macKey;
	}

	@Override
	public String hashDirectoryId(String cleartextDirectoryId) {
		final byte[] cleartextBytes = cleartextDirectoryId.getBytes(StandardCharsets.UTF_8);
		byte[] encryptedBytes = AES_SIV.encrypt(encryptionKey, macKey, cleartextBytes);
		final byte[] hashedBytes = SHA256.get().digest(encryptedBytes);
		return BASE32.encodeAsString(hashedBytes);
	}

	@Override
	public String encryptFilename(String cleartextName) {
		final byte[] cleartextBytes = cleartextName.getBytes(StandardCharsets.UTF_8);
		final byte[] encryptedBytes = AES_SIV.encrypt(encryptionKey, macKey, cleartextBytes);
		return BASE32.encodeAsString(encryptedBytes);
	}

	@Override
	public String decryptFilename(String ciphertextName) {
		final byte[] encryptedBytes = BASE32.decode(ciphertextName);
		try {
			final byte[] cleartextBytes = AES_SIV.decrypt(encryptionKey, macKey, encryptedBytes);
			return new String(cleartextBytes, StandardCharsets.UTF_8);
		} catch (AEADBadTagException e) {
			throw new UncheckedIOException(new CryptoException("Authentication failed.", e));
		}
	}

	private static class ThreadLocalSha256 extends ThreadLocal<MessageDigest> {

		@Override
		protected MessageDigest initialValue() {
			try {
				return MessageDigest.getInstance("SHA-256");
			} catch (NoSuchAlgorithmException e) {
				throw new AssertionError("SHA-256 exists in every JVM");
			}
		}

		@Override
		public MessageDigest get() {
			final MessageDigest messageDigest = super.get();
			messageDigest.reset();
			return messageDigest;
		}
	}

	/* ======================= destruction ======================= */

	@Override
	public void destroy() throws DestroyFailedException {
		TheDestroyer.destroyQuietly(encryptionKey);
		TheDestroyer.destroyQuietly(macKey);
	}

	@Override
	public boolean isDestroyed() {
		return encryptionKey.isDestroyed() && macKey.isDestroyed();
	}

}

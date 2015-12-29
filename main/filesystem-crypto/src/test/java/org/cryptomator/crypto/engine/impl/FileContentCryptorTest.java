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
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;
import org.cryptomator.io.ByteBuffers;
import org.junit.Assert;
import org.junit.Test;

public class FileContentCryptorTest {

	private static final SecureRandom RANDOM_MOCK = new SecureRandom() {

		private static final long serialVersionUID = 1505563778398085504L;

		@Override
		public void nextBytes(byte[] bytes) {
			Arrays.fill(bytes, (byte) 0x00);
		}

	};

	@Test(expected = IllegalArgumentException.class)
	public void testShortHeaderInDecryptor() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK);

		ByteBuffer tooShortHeader = ByteBuffer.allocate(63);
		cryptor.createFileContentDecryptor(tooShortHeader);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testShortHeaderInEncryptor() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK);

		ByteBuffer tooShortHeader = ByteBuffer.allocate(63);
		cryptor.createFileContentEncryptor(Optional.of(tooShortHeader));
	}

	@Test
	public void testEncryptionAndDecryption() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK);

		ByteBuffer header = ByteBuffer.allocate(cryptor.getHeaderSize());
		ByteBuffer ciphertext = ByteBuffer.allocate(100);
		try (FileContentEncryptor encryptor = cryptor.createFileContentEncryptor(Optional.empty())) {
			encryptor.append(ByteBuffer.wrap("cleartext message".getBytes()));
			encryptor.append(FileContentCryptor.EOF);
			ByteBuffer buf;
			while ((buf = encryptor.ciphertext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, ciphertext);
			}
			ByteBuffers.copy(encryptor.getHeader(), header);
		}
		header.flip();
		ciphertext.flip();

		ByteBuffer plaintext = ByteBuffer.allocate(100);
		try (FileContentDecryptor decryptor = cryptor.createFileContentDecryptor(header)) {
			decryptor.append(ciphertext);
			decryptor.append(FileContentCryptor.EOF);
			ByteBuffer buf;
			while ((buf = decryptor.cleartext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, plaintext);
			}
		}
		plaintext.flip();

		byte[] result = new byte[plaintext.remaining()];
		plaintext.get(result);
		Assert.assertArrayEquals("cleartext message".getBytes(), result);
	}
}

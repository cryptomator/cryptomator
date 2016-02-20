/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;
import org.cryptomator.io.ByteBuffers;
import org.junit.Assert;
import org.junit.Test;

public class FileContentEncryptorImplTest {

	private static final SecureRandom RANDOM_MOCK = new SecureRandom() {

		@Override
		public void nextBytes(byte[] bytes) {
			Arrays.fill(bytes, (byte) 0x00);
		}

	};

	private static final SecureRandom RANDOM_MOCK_2 = new SecureRandom() {

		@Override
		public int nextInt(int bound) {
			return 42;
		}

		@Override
		public void nextBytes(byte[] bytes) {
			Arrays.fill(bytes, (byte) 0x00);
		}

	};

	@Test
	public void testEncryption() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "AES");

		try (FileContentEncryptor encryptor = new FileContentEncryptorImpl(headerKey, macKey, RANDOM_MOCK, 0)) {
			encryptor.append(ByteBuffer.wrap("hello ".getBytes()));
			encryptor.append(ByteBuffer.wrap("world".getBytes()));
			encryptor.append(FileContentCryptor.EOF);

			ByteBuffer result = ByteBuffer.allocate(59); // 16 bytes iv + 11 bytes ciphertext + 32 bytes mac.
			ByteBuffer buf;
			while ((buf = encryptor.ciphertext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, result);
			}

			// # CIPHERTEXT:
			// echo -n "hello world" | openssl enc -aes-256-ctr -K 0000000000000000000000000000000000000000000000000000000000000000 -iv 00000000000000000000000000000000 | base64
			//
			// # MAC:
			// # 0x00-bytes for IV + blocknumber + nonce: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==
			// echo -n "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==" | base64 --decode > A; echo -n "tPCsFM1g/ubfJMY=" | base64 --decode >> A;
			// cat A | openssl dgst -sha256 -mac HMAC -macopt hexkey:0000000000000000000000000000000000000000000000000000000000000000 -binary | base64
			//
			// # FULL CHUNK:
			// echo -n "AAAAAAAAAAAAAAAAAAAAAA==" | base64 --decode > B;
			// echo -n "tPCsFM1g/ubfJMY=" | base64 --decode >> B;
			// echo -n "Klhka9WPvX1Lpn5EYfVxlyX1ISgRXtdRnivM7r6F3Og=" | base64 --decode >> B;
			// cat B | base64
			Assert.assertArrayEquals(Base64.decode("AAAAAAAAAAAAAAAAAAAAALTwrBTNYP7m3yTGKlhka9WPvX1Lpn5EYfVxlyX1ISgRXtdRnivM7r6F3Og="), result.array());
		}
	}

	@Test(expected = UncheckedIOException.class)
	public void testPassthroughException() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "AES");

		try (FileContentEncryptor encryptor = new FileContentEncryptorImpl(headerKey, macKey, RANDOM_MOCK, 0)) {
			encryptor.cancelWithException(new IOException("can not do"));
			encryptor.ciphertext();
		}
	}

	@Test
	public void testSizeObfuscation() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "AES");

		try (FileContentEncryptor encryptor = new FileContentEncryptorImpl(headerKey, macKey, RANDOM_MOCK_2, 0)) {
			encryptor.append(FileContentCryptor.EOF);

			ByteBuffer result = ByteBuffer.allocate(91); // 16 bytes iv + 42 bytes size obfuscation + 32 bytes mac + 1
			ByteBuffer buf;
			while ((buf = encryptor.ciphertext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, result);
			}
			result.flip();

			Assert.assertEquals(90, result.remaining());
		}
	}

}

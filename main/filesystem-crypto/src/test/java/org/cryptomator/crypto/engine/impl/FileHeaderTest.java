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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;
import org.cryptomator.crypto.engine.AuthenticationFailedException;
import org.junit.Assert;
import org.junit.Test;

public class FileHeaderTest {

	private static final SecureRandom RANDOM_MOCK = new SecureRandom() {

		private static final long serialVersionUID = 1505563778398085504L;

		@Override
		public void nextBytes(byte[] bytes) {
			Arrays.fill(bytes, (byte) 0x00);
		}

	};

	@Test
	public void testEncryption() {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final FileHeader header = new FileHeader(RANDOM_MOCK);
		header.getPayload().setFilesize(42);
		Assert.assertArrayEquals(new byte[16], header.getIv());
		Assert.assertArrayEquals(new byte[32], header.getPayload().getContentKey().getEncoded());
		final ByteBuffer headerAsByteBuffer = header.toByteBuffer(headerKey, new ThreadLocalMac(macKey, "HmacSHA256"));

		// 16 bytes 0x00 (IV)
		// + 48 bytes encrypted payload (see FileHeaderPayloadTest)
		// + 32 bytes HMAC of both (openssl dgst -sha256 -mac HMAC -macopt hexkey:0000000000000000000000000000000000000000000000000000000000000000 -binary)
		final String expected = "AAAAAAAAAAAAAAAAAAAAANyVwHiiQImjrUiiFJKEIIdTD4r7x0U2ualjtPHEy3OLzqdAPU1ga26lJzstK9RUv1hj5zDC4wC9FgMfoVE1mD0HnuENuYXkJA==";
		Assert.assertArrayEquals(Base64.decode(expected), Arrays.copyOf(headerAsByteBuffer.array(), headerAsByteBuffer.remaining()));
	}

	@Test
	public void testDecryption() {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final ByteBuffer headerBuf = ByteBuffer.wrap(Base64.decode("AAAAAAAAAAAAAAAAAAAAANyVwHiiQImjrUiiFJKEIIdTD4r7x0U2ualjtPHEy3OLzqdAPU1ga26lJzstK9RUv1hj5zDC4wC9FgMfoVE1mD0HnuENuYXkJA=="));
		final FileHeader header = FileHeader.decrypt(headerKey, new ThreadLocalMac(macKey, "HmacSHA256"), headerBuf);

		Assert.assertEquals(42, header.getPayload().getFilesize());
		Assert.assertArrayEquals(new byte[16], header.getIv());
		Assert.assertArrayEquals(new byte[32], header.getPayload().getContentKey().getEncoded());
	}

	@Test(expected = AuthenticationFailedException.class)
	public void testDecryptionWithInvalidMac1() {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final ByteBuffer headerBuf = ByteBuffer.wrap(Base64.decode("AAAAAAAAAAAAAAAAAAAAANyVwHiiQImjrUiiFJKEIIdTD4r7x0U2ualjtPHEy3OLzqdAPU1ga26lJzstK9RUv1hj5zDC4wC9FgMfoVE1mD0HnuENuYXkJa=="));
		FileHeader.decrypt(headerKey, new ThreadLocalMac(macKey, "HmacSHA256"), headerBuf);
	}

	@Test(expected = AuthenticationFailedException.class)
	public void testDecryptionWithInvalidMac2() {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final ByteBuffer headerBuf = ByteBuffer.wrap(Base64.decode("aAAAAAAAAAAAAAAAAAAAANyVwHiiQImjrUiiFJKEIIdTD4r7x0U2ualjtPHEy3OLzqdAPU1ga26lJzstK9RUv1hj5zDC4wC9FgMfoVE1mD0HnuENuYXkJA=="));
		FileHeader.decrypt(headerKey, new ThreadLocalMac(macKey, "HmacSHA256"), headerBuf);
	}

}

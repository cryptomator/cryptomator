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
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;
import org.cryptomator.io.ByteBuffers;
import org.junit.Assert;
import org.junit.Test;

public class FileContentEncryptorImplTest {

	private static final SecureRandom RANDOM_MOCK = new SecureRandom() {

		private static final long serialVersionUID = 1505563778398085504L;

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

			ByteBuffer result = ByteBuffer.allocate(43); // 11 bytes ciphertext + 32 bytes mac.
			ByteBuffer buf;
			while ((buf = encryptor.ciphertext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, result);
			}

			// Ciphertext: echo -n "hello world" | openssl enc -aes-256-ctr -K 0000000000000000000000000000000000000000000000000000000000000000 -iv 00000000000000000000000000000000 | base64
			// MAC: echo -n "tPCsFM1g/ubfJMY=" | base64 --decode | openssl dgst -sha256 -mac HMAC -macopt hexkey:0000000000000000000000000000000000000000000000000000000000000000 -binary | base64
			// echo -n "tPCsFM1g/ubfJMY=" | base64 --decode > A; echo -n "vgKHHT4f1jx31zBUSXSM+j5C7kYo0iCF78Z+yFMFcx0=" | base64 --decode >> A; cat A | base64
			Assert.assertArrayEquals(Base64.decode("tPCsFM1g/ubfJMa+AocdPh/WPHfXMFRJdIz6PkLuRijSIIXvxn7IUwVzHQ=="), result.array());
		}
	}

}

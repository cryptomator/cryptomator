package org.cryptomator.crypto.engine.impl;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;
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
		FileContentEncryptor encryptor = new FileContentEncryptorImpl(headerKey, macKey, RANDOM_MOCK);

		encryptor.append(ByteBuffer.wrap("hello world".getBytes()));
		encryptor.append(FileContentEncryptor.EOF);

		ByteBuffer result = ByteBuffer.allocate(11); // we just care about the first 11 bytes, as this is the ciphertext.
		ByteBuffer buf;
		while ((buf = encryptor.ciphertext()) != FileContentEncryptor.EOF) {
			ByteBuffers.copy(buf, result);
		}

		// echo -n "hello world" | openssl enc -aes-256-ctr -K 0000000000000000000000000000000000000000000000000000000000000000 -iv 00000000000000000000000000000000 | base64
		final String expected = "tPCsFM1g/ubfJMY=";
		Assert.assertArrayEquals(Base64.decode(expected), result.array());
	}

}

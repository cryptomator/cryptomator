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
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;
import org.cryptomator.crypto.engine.AuthenticationFailedException;
import org.cryptomator.crypto.engine.FileContentCryptor;
import org.cryptomator.crypto.engine.FileContentDecryptor;
import org.cryptomator.crypto.engine.FileContentEncryptor;
import org.cryptomator.io.ByteBuffers;
import org.junit.Assert;
import org.junit.Test;

public class FileContentDecryptorImplTest {

	private static final SecureRandom RANDOM_MOCK = new SecureRandom() {

		private static final long serialVersionUID = 1505563778398085504L;

		@Override
		public void nextBytes(byte[] bytes) {
			Arrays.fill(bytes, (byte) 0x00);
		}

	};

	@Test
	public void testDecryption() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final byte[] header = Base64.decode("AAAAAAAAAAAAAAAAAAAAANyVwHiiQImjrUiiFJKEIIdTD4r7x0U2ualjtPHEy3OLzqdAPU1ga26lJzstK9RUv1hj5zDC4wC9FgMfoVE1mD0HnuENuYXkJA==");
		final byte[] content = Base64.decode("AAAAAAAAAAAAAAAAAAAAALTwrBTNYP7m3yTGKlhka9WPvX1Lpn5EYfVxlyX1ISgRXtdRnivM7r6F3Og=");

		try (FileContentDecryptor decryptor = new FileContentDecryptorImpl(headerKey, macKey, ByteBuffer.wrap(header), 0, true)) {
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 0, 15)));
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 15, 59)));
			decryptor.append(FileContentCryptor.EOF);

			ByteBuffer result = ByteBuffer.allocate(11); // we just care about the first 11 bytes, as this is the ciphertext.
			ByteBuffer buf;
			while ((buf = decryptor.cleartext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, result);
			}

			Assert.assertArrayEquals("hello world".getBytes(), result.array());
		}
	}

	@Test(expected = AuthenticationFailedException.class)
	public void testManipulatedHeaderDecryption() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final byte[] header = Base64.decode("AAAAAAAAAAAAAAAAAAAAANyVwHiiQImjrUiiFJKEIIdTD4r7x0U2ualjtPHEy3OLzqdAPU1ga26lJzstK9RUv1hj5zDC4wC9FgMfoVE1mD0HnuENuYXkJa==");

		try (FileContentDecryptor decryptor = new FileContentDecryptorImpl(headerKey, macKey, ByteBuffer.wrap(header), 0, true)) {

		}
	}

	@Test(expected = AuthenticationFailedException.class)
	public void testManipulatedContentDecryption() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final byte[] header = Base64.decode("AAAAAAAAAAAAAAAAAAAAANyVwHiiQImjrUiiFJKEIIdTD4r7x0U2ualjtPHEy3OLzqdAPU1ga26lJzstK9RUv1hj5zDC4wC9FgMfoVE1mD0HnuENuYXkJA==");
		final byte[] content = Base64.decode("aAAAAAAAAAAAAAAAAAAAALTwrBTNYP7m3yTGKlhka9WPvX1Lpn5EYfVxlyX1ISgRXtdRnivM7r6F3Og=");

		try (FileContentDecryptor decryptor = new FileContentDecryptorImpl(headerKey, macKey, ByteBuffer.wrap(header), 0, true)) {
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 0, 15)));
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 15, 59)));
			decryptor.append(FileContentCryptor.EOF);

			ByteBuffer result = ByteBuffer.allocate(11); // we just care about the first 11 bytes, as this is the ciphertext.
			ByteBuffer buf;
			while ((buf = decryptor.cleartext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, result);
			}
		}
	}

	@Test
	public void testManipulatedDecryptionWithSuppressedAuthentication() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final byte[] header = Base64.decode("AAAAAAAAAAAAAAAAAAAAANyVwHiiQImjrUiiFJKEIIdTD4r7x0U2ualjtPHEy3OLzqdAPU1ga26lJzstK9RUv1hj5zDC4wC9FgMfoVE1mD0HnuENuYXkJA==");
		final byte[] content = Base64.decode("AAAAAAAAAAAAAAAAAAAAALTwrBTNYP7m3yTGKlhka9WPvX1Lpn5EYfVxlyX1ISgRXtdRnivM7r6F3OG=");

		try (FileContentDecryptor decryptor = new FileContentDecryptorImpl(headerKey, macKey, ByteBuffer.wrap(header), 0, false)) {
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 0, 15)));
			decryptor.append(ByteBuffer.wrap(Arrays.copyOfRange(content, 15, 59)));
			decryptor.append(FileContentCryptor.EOF);

			ByteBuffer result = ByteBuffer.allocate(11); // we just care about the first 11 bytes, as this is the ciphertext.
			ByteBuffer buf;
			while ((buf = decryptor.cleartext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, result);
			}

			Assert.assertArrayEquals("hello world".getBytes(), result.array());
		}
	}

	@Test(expected = UncheckedIOException.class)
	public void testPassthroughException() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey headerKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "AES");
		final byte[] header = Base64.decode("AAAAAAAAAAAAAAAAAAAAANyVwHiiQImjrUiiFJKEIIdTD4r7x0U2ualjtPHEy3OLzqdAPU1ga26lJzstK9RUv1hj5zDC4wC9FgMfoVE1mD0HnuENuYXkJA==");

		try (FileContentDecryptor decryptor = new FileContentDecryptorImpl(headerKey, macKey, ByteBuffer.wrap(header), 0, true)) {
			decryptor.cancelWithException(new IOException("can not do"));
			decryptor.cleartext();
		}
	}

	@Test(timeout = 200000)
	public void testPartialDecryption() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK);

		ByteBuffer header = ByteBuffer.allocate(cryptor.getHeaderSize());
		ByteBuffer ciphertext = ByteBuffer.allocate(131264); // 4 * (16 + 32k + 32)
		try (FileContentEncryptor encryptor = cryptor.createFileContentEncryptor(Optional.empty(), 0)) {
			final Thread ciphertextWriter = new Thread(() -> {
				ByteBuffer buf;
				try {
					while ((buf = encryptor.ciphertext()) != FileContentCryptor.EOF) {
						ByteBuffers.copy(buf, ciphertext);
					}
					ciphertext.flip();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			ciphertextWriter.start();

			// write cleartext:
			ByteBuffer intBuf = ByteBuffer.allocate(32768);
			for (int i = 0; i < 4; i++) {
				intBuf.clear();
				intBuf.putInt(i);
				intBuf.rewind();
				encryptor.append(intBuf);
			}
			encryptor.append(FileContentCryptor.EOF);
			ciphertextWriter.join();
			header = encryptor.getHeader();
		}

		for (int i = 3; i >= 0; i--) {
			final int ciphertextPos = (int) cryptor.toCiphertextPos(i * 32768);
			try (FileContentDecryptor decryptor = cryptor.createFileContentDecryptor(header, ciphertextPos, true)) {
				final Thread ciphertextReader = new Thread(() -> {
					try {
						ciphertext.position(ciphertextPos);
						decryptor.append(ciphertext);
						decryptor.append(FileContentCryptor.EOF);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
				ciphertextReader.start();

				// read cleartext:
				ByteBuffer decrypted = decryptor.cleartext();
				Assert.assertEquals(i, decrypted.getInt());

				ciphertextReader.interrupt();
			}
		}

	}

}

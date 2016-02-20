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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileContentCryptorImplTest {

	private static final Logger LOG = LoggerFactory.getLogger(FileContentCryptorImplTest.class);

	private static final SecureRandom RANDOM_MOCK = new SecureRandom() {

		@Override
		public void nextBytes(byte[] bytes) {
			Arrays.fill(bytes, (byte) 0x00);
		}

	};

	private static final SecureRandom RANDOM_MOCK_2 = new SecureRandom() {

		@Override
		public int nextInt(int bound) {
			return 500;
		}

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
		cryptor.createFileContentDecryptor(tooShortHeader, 0, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testShortHeaderInEncryptor() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK);

		ByteBuffer tooShortHeader = ByteBuffer.allocate(63);
		cryptor.createFileContentEncryptor(Optional.of(tooShortHeader), 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidStartingPointInDecryptor() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK);

		ByteBuffer header = ByteBuffer.allocate(cryptor.getHeaderSize());
		cryptor.createFileContentDecryptor(header, 3, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidStartingPointEncryptor() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK);

		cryptor.createFileContentEncryptor(Optional.empty(), 3);
	}

	@Test
	public void testEncryptionAndDecryption() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK);

		ByteBuffer header = ByteBuffer.allocate(cryptor.getHeaderSize());
		ByteBuffer ciphertext = ByteBuffer.allocate(100);
		try (FileContentEncryptor encryptor = cryptor.createFileContentEncryptor(Optional.empty(), 0)) {
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
		try (FileContentDecryptor decryptor = cryptor.createFileContentDecryptor(header, 0, true)) {
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

	@Test
	public void testEncryptionAndDecryptionWithSizeObfuscationPadding() throws InterruptedException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK_2);

		ByteBuffer header = ByteBuffer.allocate(cryptor.getHeaderSize());
		ByteBuffer ciphertext = ByteBuffer.allocate(16 + 11 + 500 + 32 + 1); // 16 bytes iv + 11 bytes ciphertext + 500 bytes padding + 32 bytes mac + 1.
		try (FileContentEncryptor encryptor = cryptor.createFileContentEncryptor(Optional.empty(), 0)) {
			encryptor.append(ByteBuffer.wrap("hello world".getBytes()));
			encryptor.append(FileContentCryptor.EOF);
			ByteBuffer buf;
			while ((buf = encryptor.ciphertext()) != FileContentCryptor.EOF) {
				ByteBuffers.copy(buf, ciphertext);
			}
			ByteBuffers.copy(encryptor.getHeader(), header);
		}
		header.flip();
		ciphertext.flip();

		Assert.assertEquals(16 + 11 + 500 + 32, ciphertext.remaining());

		ByteBuffer plaintext = ByteBuffer.allocate(12); // 11 bytes plaintext + 1
		try (FileContentDecryptor decryptor = cryptor.createFileContentDecryptor(header, 0, true)) {
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
		Assert.assertArrayEquals("hello world".getBytes(), result);
	}

	@Test(timeout = 20000) // assuming a minimum speed of 10mb/s during encryption and decryption 20s should be enough
	public void testEncryptionAndDecryptionSpeed() throws InterruptedException, IOException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "HmacSHA256");
		final FileContentCryptor cryptor = new FileContentCryptorImpl(encryptionKey, macKey, RANDOM_MOCK);
		final Path tmpFile = Files.createTempFile("encrypted", ".tmp");

		final Thread fileWriter;
		final ByteBuffer header;
		final long encStart = System.nanoTime();
		try (FileContentEncryptor encryptor = cryptor.createFileContentEncryptor(Optional.empty(), 0)) {
			fileWriter = new Thread(() -> {
				try (FileChannel fc = FileChannel.open(tmpFile, StandardOpenOption.WRITE)) {
					ByteBuffer ciphertext;
					while ((ciphertext = encryptor.ciphertext()) != FileContentCryptor.EOF) {
						fc.write(ciphertext);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			fileWriter.start();

			final ByteBuffer cleartext = ByteBuffer.allocate(100000); // 100k
			for (int i = 0; i < 1000; i++) { // 100M total
				cleartext.rewind();
				encryptor.append(cleartext);
			}
			encryptor.append(FileContentCryptor.EOF);
			header = encryptor.getHeader();
		}
		fileWriter.join();
		final long encEnd = System.nanoTime();
		LOG.debug("Encryption of 100M took {}ms", (encEnd - encStart) / 1000 / 1000);

		final Thread fileReader;
		final long decStart = System.nanoTime();
		try (FileContentDecryptor decryptor = cryptor.createFileContentDecryptor(header, 0, true)) {
			fileReader = new Thread(() -> {
				try (FileChannel fc = FileChannel.open(tmpFile, StandardOpenOption.READ)) {
					ByteBuffer ciphertext = ByteBuffer.allocate(654321);
					while (fc.read(ciphertext) != -1) {
						ciphertext.flip();
						decryptor.append(ciphertext);
						ciphertext.clear();
					}
					decryptor.append(FileContentCryptor.EOF);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			fileReader.start();

			while (decryptor.cleartext() != FileContentCryptor.EOF) {
				// no-op
			}
		}
		fileReader.join();
		final long decEnd = System.nanoTime();
		LOG.debug("Decryption of 100M took {}ms", (decEnd - decStart) / 1000 / 1000);
		Files.delete(tmpFile);
	}
}

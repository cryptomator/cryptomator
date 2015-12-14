package org.cryptomator.crypto.engine.impl;

import java.io.IOException;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.cryptomator.crypto.engine.Cryptor;
import org.junit.Assert;
import org.junit.Test;

public class FilenameCryptorImplTest {

	@Test(timeout = 1000)
	public void testDeterministicEncryptionOfFilenames() throws IOException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "AES");
		final Cryptor cryptor = new CryptorImpl(encryptionKey, macKey);

		// some random
		for (int i = 0; i < 2000; i++) {
			final String origName = UUID.randomUUID().toString();
			final String encrypted1 = cryptor.getFilenameCryptor().encryptFilename(origName);
			final String encrypted2 = cryptor.getFilenameCryptor().encryptFilename(origName);
			Assert.assertEquals(encrypted1, encrypted2);
			final String decrypted = cryptor.getFilenameCryptor().decryptFilename(encrypted1);
			Assert.assertEquals(origName, decrypted);
		}

		// block size length file names
		final String originalPath3 = "aaaabbbbccccdddd"; // 128 bit ascii
		final String encryptedPath3a = cryptor.getFilenameCryptor().encryptFilename(originalPath3);
		final String encryptedPath3b = cryptor.getFilenameCryptor().encryptFilename(originalPath3);
		Assert.assertEquals(encryptedPath3a, encryptedPath3b);
		final String decryptedPath3 = cryptor.getFilenameCryptor().decryptFilename(encryptedPath3a);
		Assert.assertEquals(originalPath3, decryptedPath3);
	}

	@Test(timeout = 1000)
	public void testDeterministicHashingOfDirectoryIds() throws IOException {
		final byte[] keyBytes = new byte[32];
		final SecretKey encryptionKey = new SecretKeySpec(keyBytes, "AES");
		final SecretKey macKey = new SecretKeySpec(keyBytes, "AES");
		final Cryptor cryptor = new CryptorImpl(encryptionKey, macKey);

		// some random
		for (int i = 0; i < 2000; i++) {
			final String originalDirectoryId = UUID.randomUUID().toString();
			final String hashedDirectory1 = cryptor.getFilenameCryptor().hashDirectoryId(originalDirectoryId);
			final String hashedDirectory2 = cryptor.getFilenameCryptor().hashDirectoryId(originalDirectoryId);
			Assert.assertEquals(hashedDirectory1, hashedDirectory2);
		}
	}

}

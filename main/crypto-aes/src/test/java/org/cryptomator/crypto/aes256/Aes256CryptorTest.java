/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.aes256;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.cryptomator.crypto.CryptorIOSupport;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class Aes256CryptorTest {

	private static final Random TEST_PRNG = new Random();

	private Path tmpDir;
	private Path masterKey;
	private Path encryptedFile;

	@Before
	public void prepareTmpDir() throws IOException {
		final String tmpDirName = (String) System.getProperties().get("java.io.tmpdir");
		final Path path = FileSystems.getDefault().getPath(tmpDirName);
		tmpDir = Files.createTempDirectory(path, "oce-crypto-test");
		masterKey = tmpDir.resolve("test" + Aes256Cryptor.MASTERKEY_FILE_EXT);
		encryptedFile = tmpDir.resolve("test" + Aes256Cryptor.BASIC_FILE_EXT);
	}

	@After
	public void dropTmpDir() {
		try {
			FileUtils.deleteDirectory(tmpDir.toFile());
		} catch (IOException e) {
			// ignore
		}
	}

	/* ------------------------------------------------------------------------------- */

	@Test
	public void testCorrectPassword() throws IOException, WrongPasswordException, DecryptFailedException, UnsupportedKeyLengthException {
		final String pw = "asd";
		final Aes256Cryptor cryptor = new Aes256Cryptor(TEST_PRNG);
		final OutputStream out = Files.newOutputStream(masterKey, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		cryptor.encryptMasterKey(out, pw);
		cryptor.swipeSensitiveData();

		final Aes256Cryptor decryptor = new Aes256Cryptor(TEST_PRNG);
		final InputStream in = Files.newInputStream(masterKey, StandardOpenOption.READ);
		decryptor.decryptMasterKey(in, pw);
	}

	@Test(expected = WrongPasswordException.class)
	public void testWrongPassword() throws IOException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException {
		final String pw = "asd";
		final Aes256Cryptor cryptor = new Aes256Cryptor(TEST_PRNG);
		final OutputStream out = Files.newOutputStream(masterKey, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		cryptor.encryptMasterKey(out, pw);
		cryptor.swipeSensitiveData();

		final String wrongPw = "foo";
		final Aes256Cryptor decryptor = new Aes256Cryptor(TEST_PRNG);
		final InputStream in = Files.newInputStream(masterKey, StandardOpenOption.READ);
		decryptor.decryptMasterKey(in, wrongPw);
	}

	@Test(expected = NoSuchFileException.class)
	public void testWrongLocation() throws IOException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException {
		final String pw = "asd";
		final Aes256Cryptor cryptor = new Aes256Cryptor(TEST_PRNG);
		final OutputStream out = Files.newOutputStream(masterKey, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		cryptor.encryptMasterKey(out, pw);
		cryptor.swipeSensitiveData();

		final Path wrongMasterKey = tmpDir.resolve("notExistingMasterKey.json");
		final Aes256Cryptor decryptor = new Aes256Cryptor(TEST_PRNG);
		final InputStream in = Files.newInputStream(wrongMasterKey, StandardOpenOption.READ);
		decryptor.decryptMasterKey(in, pw);
	}

	@Test
	public void testPartialDecryption() throws IOException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException {
		// our test plaintext data:
		final byte[] plaintextData = new byte[500 * Integer.BYTES];
		final ByteBuffer bbIn = ByteBuffer.wrap(plaintextData);
		for (int i = 0; i < 500; i++) {
			bbIn.putInt(i);
		}
		final InputStream plaintextIn = new ByteArrayInputStream(plaintextData);

		// init cryptor:
		final Aes256Cryptor cryptor = new Aes256Cryptor(TEST_PRNG);

		// encrypt:
		final SeekableByteChannel fileOut = Files.newByteChannel(encryptedFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		cryptor.encryptFile(plaintextIn, fileOut);
		fileOut.close();

		// decrypt:
		final SeekableByteChannel fileIn = Files.newByteChannel(encryptedFile, StandardOpenOption.READ);
		final ByteArrayOutputStream plaintextOut = new ByteArrayOutputStream();
		final Long numDecryptedBytes = cryptor.decryptRange(fileIn, plaintextOut, 313 * Integer.BYTES, 50 * Integer.BYTES);
		Assert.assertTrue(numDecryptedBytes > 0);

		final byte[] result = plaintextOut.toByteArray();
		final byte[] expected = new byte[50 * Integer.BYTES];
		final ByteBuffer bbOut = ByteBuffer.wrap(expected);
		for (int i = 313; i < 363; i++) {
			bbOut.putInt(i);
		}
		Assert.assertArrayEquals(expected, result);
	}

	@Test(expected = FileAlreadyExistsException.class)
	public void testReInitialization() throws IOException {
		final String pw = "asd";
		final Aes256Cryptor cryptor = new Aes256Cryptor(TEST_PRNG);
		final OutputStream out = Files.newOutputStream(masterKey, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		cryptor.encryptMasterKey(out, pw);
		cryptor.swipeSensitiveData();

		final OutputStream outAgain = Files.newOutputStream(masterKey, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
		cryptor.encryptMasterKey(outAgain, pw);
		cryptor.swipeSensitiveData();
	}

	@Test
	public void testEncryptionOfFilenames() throws IOException {
		final CryptorIOSupport ioSupportMock = new CryptoIOSupportMock();
		final Aes256Cryptor cryptor = new Aes256Cryptor(TEST_PRNG);

		// short path components
		final String originalPath1 = "foo/bar/baz";
		final String encryptedPath1 = cryptor.encryptPath(originalPath1, '/', '/', ioSupportMock);
		final String decryptedPath1 = cryptor.decryptPath(encryptedPath1, '/', '/', ioSupportMock);
		Assert.assertEquals(originalPath1, decryptedPath1);

		// long path components
		final String str50chars = "aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee";
		final String originalPath2 = "foo/" + str50chars + str50chars + str50chars + str50chars + str50chars + "/baz";
		final String encryptedPath2 = cryptor.encryptPath(originalPath2, '/', '/', ioSupportMock);
		final String decryptedPath2 = cryptor.decryptPath(encryptedPath2, '/', '/', ioSupportMock);
		Assert.assertEquals(originalPath2, decryptedPath2);
	}

	private static class CryptoIOSupportMock implements CryptorIOSupport {

		private final Map<String, byte[]> map = new HashMap<>();

		@Override
		public void writePathSpecificMetadata(String encryptedPath, byte[] encryptedMetadata) {
			map.put(encryptedPath, encryptedMetadata);
		}

		@Override
		public byte[] readPathSpecificMetadata(String encryptedPath) {
			return map.get(encryptedPath);
		}

	}

}

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
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.cryptomator.crypto.CryptorIOSupport;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.junit.Assert;
import org.junit.Test;

public class Aes256CryptorTest {

	@Test
	public void testCorrectPassword() throws IOException, WrongPasswordException, DecryptFailedException, UnsupportedKeyLengthException {
		final String pw = "asd";
		final Aes256Cryptor cryptor = new Aes256Cryptor();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		cryptor.encryptMasterKey(out, pw);
		cryptor.swipeSensitiveData();

		final Aes256Cryptor decryptor = new Aes256Cryptor();
		final InputStream in = new ByteArrayInputStream(out.toByteArray());
		decryptor.decryptMasterKey(in, pw);

		IOUtils.closeQuietly(out);
		IOUtils.closeQuietly(in);
	}

	@Test
	public void testWrongPassword() throws IOException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException {
		final String pw = "asd";
		final Aes256Cryptor cryptor = new Aes256Cryptor();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		cryptor.encryptMasterKey(out, pw);
		cryptor.swipeSensitiveData();
		IOUtils.closeQuietly(out);

		// all these passwords are expected to fail.
		final String[] wrongPws = {"a", "as", "asdf", "sdf", "das", "dsa", "foo", "bar", "baz"};
		final Aes256Cryptor decryptor = new Aes256Cryptor();
		for (final String wrongPw : wrongPws) {
			final InputStream in = new ByteArrayInputStream(out.toByteArray());
			try {
				decryptor.decryptMasterKey(in, wrongPw);
				Assert.fail("should not succeed.");
			} catch (WrongPasswordException e) {
				continue;
			} finally {
				IOUtils.closeQuietly(in);
			}
		}
	}

	@Test
	public void testIntegrityAuthentication() throws IOException, DecryptFailedException, EncryptFailedException {
		// our test plaintext data:
		final byte[] plaintextData = "Hello World".getBytes();
		final InputStream plaintextIn = new ByteArrayInputStream(plaintextData);

		// init cryptor:
		final Aes256Cryptor cryptor = new Aes256Cryptor();

		// encrypt:
		final ByteBuffer encryptedData = ByteBuffer.allocate(96);
		final SeekableByteChannel encryptedOut = new ByteBufferBackedSeekableChannel(encryptedData);
		cryptor.encryptFile(plaintextIn, encryptedOut);
		IOUtils.closeQuietly(plaintextIn);
		IOUtils.closeQuietly(encryptedOut);

		encryptedData.position(0);

		// toggle one bit inf first content byte:
		encryptedData.position(64);
		final byte fifthByte = encryptedData.get();
		encryptedData.position(64);
		encryptedData.put((byte) (fifthByte ^ 0x01));

		encryptedData.position(0);

		// check mac (should return false)
		final SeekableByteChannel encryptedIn = new ByteBufferBackedSeekableChannel(encryptedData);
		final boolean authentic = cryptor.isAuthentic(encryptedIn);
		Assert.assertFalse(authentic);
	}

	@Test(expected = DecryptFailedException.class)
	public void testIntegrityViolationDuringDecryption() throws IOException, DecryptFailedException, EncryptFailedException {
		// our test plaintext data:
		final byte[] plaintextData = "Hello World".getBytes();
		final InputStream plaintextIn = new ByteArrayInputStream(plaintextData);

		// init cryptor:
		final Aes256Cryptor cryptor = new Aes256Cryptor();

		// encrypt:
		final ByteBuffer encryptedData = ByteBuffer.allocate(96);
		final SeekableByteChannel encryptedOut = new ByteBufferBackedSeekableChannel(encryptedData);
		cryptor.encryptFile(plaintextIn, encryptedOut);
		IOUtils.closeQuietly(plaintextIn);
		IOUtils.closeQuietly(encryptedOut);

		encryptedData.position(0);

		// toggle one bit inf first content byte:
		encryptedData.position(64);
		final byte fifthByte = encryptedData.get();
		encryptedData.position(64);
		encryptedData.put((byte) (fifthByte ^ 0x01));

		encryptedData.position(0);

		// decrypt modified content (should fail with DecryptFailedException):
		final SeekableByteChannel encryptedIn = new ByteBufferBackedSeekableChannel(encryptedData);
		final ByteArrayOutputStream plaintextOut = new ByteArrayOutputStream();
		cryptor.decryptFile(encryptedIn, plaintextOut);
	}

	@Test
	public void testEncryptionAndDecryption() throws IOException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, EncryptFailedException {
		// our test plaintext data:
		final byte[] plaintextData = "Hello World".getBytes();
		final InputStream plaintextIn = new ByteArrayInputStream(plaintextData);

		// init cryptor:
		final Aes256Cryptor cryptor = new Aes256Cryptor();

		// encrypt:
		final ByteBuffer encryptedData = ByteBuffer.allocate(96);
		final SeekableByteChannel encryptedOut = new ByteBufferBackedSeekableChannel(encryptedData);
		cryptor.encryptFile(plaintextIn, encryptedOut);
		IOUtils.closeQuietly(plaintextIn);
		IOUtils.closeQuietly(encryptedOut);

		encryptedData.position(0);

		// decrypt file size:
		final SeekableByteChannel encryptedIn = new ByteBufferBackedSeekableChannel(encryptedData);
		final Long filesize = cryptor.decryptedContentLength(encryptedIn);
		Assert.assertEquals(plaintextData.length, filesize.longValue());

		// decrypt:
		final ByteArrayOutputStream plaintextOut = new ByteArrayOutputStream();
		final Long numDecryptedBytes = cryptor.decryptFile(encryptedIn, plaintextOut);
		IOUtils.closeQuietly(encryptedIn);
		IOUtils.closeQuietly(plaintextOut);
		Assert.assertEquals(filesize.longValue(), numDecryptedBytes.longValue());

		// check decrypted data:
		final byte[] result = plaintextOut.toByteArray();
		Assert.assertArrayEquals(plaintextData, result);
	}

	@Test
	public void testPartialDecryption() throws IOException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, EncryptFailedException {
		// our test plaintext data:
		final byte[] plaintextData = new byte[65536 * Integer.BYTES];
		final ByteBuffer bbIn = ByteBuffer.wrap(plaintextData);
		for (int i = 0; i < 65536; i++) {
			bbIn.putInt(i);
		}
		final InputStream plaintextIn = new ByteArrayInputStream(plaintextData);

		// init cryptor:
		final Aes256Cryptor cryptor = new Aes256Cryptor();

		// encrypt:
		final ByteBuffer encryptedData = ByteBuffer.allocate((int) (64 + plaintextData.length * 1.2));
		final SeekableByteChannel encryptedOut = new ByteBufferBackedSeekableChannel(encryptedData);
		cryptor.encryptFile(plaintextIn, encryptedOut);
		IOUtils.closeQuietly(plaintextIn);
		IOUtils.closeQuietly(encryptedOut);

		encryptedData.position(0);

		// decrypt:
		final SeekableByteChannel encryptedIn = new ByteBufferBackedSeekableChannel(encryptedData);
		final ByteArrayOutputStream plaintextOut = new ByteArrayOutputStream();
		final Long numDecryptedBytes = cryptor.decryptRange(encryptedIn, plaintextOut, 25000 * Integer.BYTES, 30000 * Integer.BYTES);
		IOUtils.closeQuietly(encryptedIn);
		IOUtils.closeQuietly(plaintextOut);
		Assert.assertTrue(numDecryptedBytes > 0);

		// check decrypted data:
		final byte[] result = plaintextOut.toByteArray();
		final byte[] expected = Arrays.copyOfRange(plaintextData, 25000 * Integer.BYTES, 55000 * Integer.BYTES);
		Assert.assertArrayEquals(expected, result);
	}

	@Test
	public void testEncryptionOfFilenames() throws IOException, DecryptFailedException {
		final CryptorIOSupport ioSupportMock = new CryptoIOSupportMock();
		final Aes256Cryptor cryptor = new Aes256Cryptor();

		// short path components
		final String originalPath1 = "foo/bar/baz";
		final String encryptedPath1a = cryptor.encryptPath(originalPath1, '/', '/', ioSupportMock);
		final String encryptedPath1b = cryptor.encryptPath(originalPath1, '/', '/', ioSupportMock);
		Assert.assertEquals(encryptedPath1a, encryptedPath1b);
		final String decryptedPath1 = cryptor.decryptPath(encryptedPath1a, '/', '/', ioSupportMock);
		Assert.assertEquals(originalPath1, decryptedPath1);

		// long path components
		final String str50chars = "aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee";
		final String originalPath2 = "foo/" + str50chars + str50chars + str50chars + str50chars + str50chars + "/baz";
		final String encryptedPath2a = cryptor.encryptPath(originalPath2, '/', '/', ioSupportMock);
		final String encryptedPath2b = cryptor.encryptPath(originalPath2, '/', '/', ioSupportMock);
		Assert.assertEquals(encryptedPath2a, encryptedPath2b);
		final String decryptedPath2 = cryptor.decryptPath(encryptedPath2a, '/', '/', ioSupportMock);
		Assert.assertEquals(originalPath2, decryptedPath2);

		// block size length path components
		final String originalPath3 = "aaaabbbbccccdddd";
		final String encryptedPath3a = cryptor.encryptPath(originalPath3, '/', '/', ioSupportMock);
		final String encryptedPath3b = cryptor.encryptPath(originalPath3, '/', '/', ioSupportMock);
		Assert.assertEquals(encryptedPath3a, encryptedPath3b);
		final String decryptedPath3 = cryptor.decryptPath(encryptedPath3a, '/', '/', ioSupportMock);
		Assert.assertEquals(originalPath3, decryptedPath3);
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

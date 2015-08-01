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

import javax.security.auth.DestroyFailedException;

import org.apache.commons.io.IOUtils;
import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.UnsupportedVaultException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;
import org.junit.Assert;
import org.junit.Test;

public class Aes256CryptorTest {

	@Test
	public void testCorrectPassword() throws IOException, WrongPasswordException, DecryptFailedException, UnsupportedKeyLengthException, DestroyFailedException, UnsupportedVaultException {
		final String pw = "asd";
		final Aes256Cryptor cryptor = new Aes256Cryptor();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		cryptor.encryptMasterKey(out, pw);
		cryptor.destroy();

		final Aes256Cryptor decryptor = new Aes256Cryptor();
		final InputStream in = new ByteArrayInputStream(out.toByteArray());
		decryptor.decryptMasterKey(in, pw);

		IOUtils.closeQuietly(out);
		IOUtils.closeQuietly(in);
	}

	@Test
	public void testWrongPassword() throws IOException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, DestroyFailedException, UnsupportedVaultException {
		final String pw = "asd";
		final Aes256Cryptor cryptor = new Aes256Cryptor();
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		cryptor.encryptMasterKey(out, pw);
		cryptor.destroy();
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

	@Test(expected = DecryptFailedException.class)
	public void testIntegrityViolationDuringDecryption() throws IOException, DecryptFailedException, EncryptFailedException {
		// our test plaintext data:
		final byte[] plaintextData = "Hello World".getBytes();
		final InputStream plaintextIn = new ByteArrayInputStream(plaintextData);

		// init cryptor:
		final Aes256Cryptor cryptor = new Aes256Cryptor();

		// encrypt:
		final ByteBuffer encryptedData = ByteBuffer.allocate(104 + plaintextData.length + 4096);
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
		cryptor.decryptFile(encryptedIn, plaintextOut, true);
	}

	@Test
	public void testEncryptionAndDecryption() throws IOException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, EncryptFailedException {
		// our test plaintext data:
		final byte[] plaintextData = "Hello World".getBytes();
		final InputStream plaintextIn = new ByteArrayInputStream(plaintextData);

		// init cryptor:
		final Aes256Cryptor cryptor = new Aes256Cryptor();

		// encrypt:
		final ByteBuffer encryptedData = ByteBuffer.allocate(104 + plaintextData.length + 4096);
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
		final Long numDecryptedBytes = cryptor.decryptFile(encryptedIn, plaintextOut, true);
		IOUtils.closeQuietly(encryptedIn);
		IOUtils.closeQuietly(plaintextOut);
		Assert.assertEquals(filesize.longValue(), numDecryptedBytes.longValue());

		// check decrypted data:
		final byte[] result = plaintextOut.toByteArray();
		Assert.assertArrayEquals(plaintextData, result);
	}

	@Test
	public void testPartialDecryption() throws IOException, DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, EncryptFailedException {
		// 8MiB test plaintext data:
		final byte[] plaintextData = new byte[2097152 * Integer.BYTES];
		final ByteBuffer bbIn = ByteBuffer.wrap(plaintextData);
		for (int i = 0; i < 2097152; i++) {
			bbIn.putInt(i);
		}
		final InputStream plaintextIn = new ByteArrayInputStream(plaintextData);

		// init cryptor:
		final Aes256Cryptor cryptor = new Aes256Cryptor();

		// encrypt:
		final ByteBuffer encryptedData = ByteBuffer.allocate((int) (104 + plaintextData.length * 1.2));
		final SeekableByteChannel encryptedOut = new ByteBufferBackedSeekableChannel(encryptedData);
		cryptor.encryptFile(plaintextIn, encryptedOut);
		IOUtils.closeQuietly(plaintextIn);
		IOUtils.closeQuietly(encryptedOut);

		encryptedData.position(0);

		// decrypt:
		final SeekableByteChannel encryptedIn = new ByteBufferBackedSeekableChannel(encryptedData);
		final ByteArrayOutputStream plaintextOut = new ByteArrayOutputStream();
		final Long numDecryptedBytes = cryptor.decryptRange(encryptedIn, plaintextOut, 260000 * Integer.BYTES, 4000 * Integer.BYTES, true);
		IOUtils.closeQuietly(encryptedIn);
		IOUtils.closeQuietly(plaintextOut);
		Assert.assertTrue(numDecryptedBytes > 0);

		// check decrypted data:
		final byte[] result = plaintextOut.toByteArray();
		final byte[] expected = Arrays.copyOfRange(plaintextData, 260000 * Integer.BYTES, 264000 * Integer.BYTES);
		Assert.assertArrayEquals(expected, result);
	}

	@Test
	public void testEncryptionOfFilenames() throws IOException, DecryptFailedException {
		final Aes256Cryptor cryptor = new Aes256Cryptor();

		// directory paths
		final String originalPath1 = "foo/bar/baz";
		final String encryptedPath1a = cryptor.encryptDirectoryPath(originalPath1, "/");
		final String encryptedPath1b = cryptor.encryptDirectoryPath(originalPath1, "/");
		Assert.assertEquals(encryptedPath1a, encryptedPath1b);

		// long file names
		final String str50chars = "aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeee";
		final String originalPath2 = str50chars + str50chars + str50chars + str50chars + str50chars + "_isLongerThan255Chars.txt";
		final String encryptedPath2a = cryptor.encryptFilename(originalPath2);
		final String encryptedPath2b = cryptor.encryptFilename(originalPath2);
		Assert.assertEquals(encryptedPath2a, encryptedPath2b);
		final String decryptedPath2 = cryptor.decryptFilename(encryptedPath2a);
		Assert.assertEquals(originalPath2, decryptedPath2);

		// block size length file names
		final String originalPath3 = "aaaabbbbccccdddd";
		final String encryptedPath3a = cryptor.encryptFilename(originalPath3);
		final String encryptedPath3b = cryptor.encryptFilename(originalPath3);
		Assert.assertEquals(encryptedPath3a, encryptedPath3b);
		final String decryptedPath3 = cryptor.decryptFilename(encryptedPath3a);
		Assert.assertEquals(originalPath3, decryptedPath3);
	}

}

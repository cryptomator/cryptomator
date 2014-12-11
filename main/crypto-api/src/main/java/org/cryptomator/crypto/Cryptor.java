/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;

import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;

/**
 * Provides access to cryptographic functions. All methods are threadsafe.
 */
public interface Cryptor extends SensitiveDataSwipeListener {

	/**
	 * Encrypts the current masterKey with the given password and writes the result to the given output stream.
	 */
	void encryptMasterKey(OutputStream out, CharSequence password) throws IOException;

	/**
	 * Reads the encrypted masterkey from the given input stream and decrypts it with the given password.
	 * 
	 * @throws DecryptFailedException If the decryption failed for various reasons (including wrong password).
	 * @throws WrongPasswordException If the provided password was wrong. Note: Sometimes the algorithm itself fails due to a wrong
	 *             password. In this case a DecryptFailedException will be thrown.
	 * @throws UnsupportedKeyLengthException If the masterkey has been encrypted with a higher key length than supported by the system. In
	 *             this case Java JCE needs to be installed.
	 */
	void decryptMasterKey(InputStream in, CharSequence password) throws DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException;

	/**
	 * Encrypts each plaintext path component for its own.
	 * 
	 * @param cleartextPath A relative path (UTF-8 encoded)
	 * @param encryptedPathSep Path separator char like '/' used on local file system. Must not be null, even if cleartextPath is a sole
	 *            file name without any path separators.
	 * @param cleartextPathSep Path separator char like '/' used in webdav URIs. Must not be null, even if cleartextPath is a sole file name
	 *            without any path separators.
	 * @param metadataSupport Support object allowing the Cryptor to read and write its own metadata to the location of the encrypted file.
	 * @return Encrypted path components concatenated by the given encryptedPathSep. Must not start with encryptedPathSep, unless the
	 *         encrypted path is explicitly absolute.
	 */
	String encryptPath(String cleartextPath, char encryptedPathSep, char cleartextPathSep, CryptorIOSupport ioSupport);

	/**
	 * Decrypts each encrypted path component for its own.
	 * 
	 * @param encryptedPath A relative path (UTF-8 encoded)
	 * @param encryptedPathSep Path separator char like '/' used on local file system. Must not be null, even if encryptedPath is a sole
	 *            file name without any path separators.
	 * @param cleartextPathSep Path separator char like '/' used in webdav URIs. Must not be null, even if encryptedPath is a sole file name
	 *            without any path separators.
	 * @param metadataSupport Support object allowing the Cryptor to read and write its own metadata to the location of the encrypted file.
	 * @return Decrypted path components concatenated by the given cleartextPathSep. Must not start with cleartextPathSep, unless the
	 *         cleartext path is explicitly absolute.
	 */
	String decryptPath(String encryptedPath, char encryptedPathSep, char cleartextPathSep, CryptorIOSupport ioSupport);

	/**
	 * @param metadataSupport Support object allowing the Cryptor to read and write its own metadata to the location of the encrypted file.
	 * @return Content length of the decrypted file or <code>null</code> if unknown.
	 */
	Long decryptedContentLength(SeekableByteChannel encryptedFile) throws IOException;

	/**
	 * @return Number of decrypted bytes. This might not be equal to the encrypted file size due to optional metadata written to it.
	 */
	Long decryptedFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile) throws IOException;

	/**
	 * @return Number of encrypted bytes. This might not be equal to the encrypted file size due to optional metadata written to it.
	 */
	Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException;

	/**
	 * @return A filter, that returns <code>true</code> for encrypted files, i.e. if the file is an actual user payload and not a supporting
	 *         metadata file of the {@link Cryptor}.
	 */
	Filter<Path> getPayloadFilesFilter();

	void addSensitiveDataSwipeListener(SensitiveDataSwipeListener listener);

	void removeSensitiveDataSwipeListener(SensitiveDataSwipeListener listener);

}

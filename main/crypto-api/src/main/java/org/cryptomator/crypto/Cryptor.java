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

import javax.security.auth.Destroyable;

import org.cryptomator.crypto.exceptions.DecryptFailedException;
import org.cryptomator.crypto.exceptions.EncryptFailedException;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;
import org.cryptomator.crypto.exceptions.UnsupportedKeyLengthException;
import org.cryptomator.crypto.exceptions.UnsupportedVaultException;
import org.cryptomator.crypto.exceptions.WrongPasswordException;

/**
 * Provides access to cryptographic functions. All methods are threadsafe.
 */
public interface Cryptor extends Destroyable {

	/**
	 * Encrypts the current masterKey with the given password and writes the result to the given output stream.
	 */
	void encryptMasterKey(OutputStream out, CharSequence password) throws IOException;

	/**
	 * Reads the encrypted masterkey from the given input stream and decrypts it with the given password.
	 * 
	 * @throws DecryptFailedException If the decryption failed for various reasons (including wrong password).
	 * @throws WrongPasswordException If the provided password was wrong. Note: Sometimes the algorithm itself fails due to a wrong password. In this case a DecryptFailedException will be thrown.
	 * @throws UnsupportedKeyLengthException If the masterkey has been encrypted with a higher key length than supported by the system. In this case Java JCE needs to be installed.
	 * @throws UnsupportedVaultException If the masterkey file is too old or too modern.
	 */
	void decryptMasterKey(InputStream in, CharSequence password) throws DecryptFailedException, WrongPasswordException, UnsupportedKeyLengthException, IOException, UnsupportedVaultException;

	/**
	 * Encrypts a given plaintext path representing a directory structure. See {@link #encryptFilename(String, CryptorMetadataSupport)} for contents inside directories.
	 * 
	 * @param cleartextDirectoryId A unique directory id
	 * @param nativePathSep Path separator like "/" used on local file system. Must not be null, even if cleartextPath is a sole file name without any path separators.
	 * @return Encrypted path.
	 */
	String encryptDirectoryPath(String cleartextDirectoryId, String nativePathSep);

	/**
	 * Encrypts the name of a file. See {@link #encryptDirectoryPath(String, char)} for parent dir.
	 * 
	 * @param cleartextName A plaintext filename without any preceeding directory paths.
	 * @return Encrypted filename.
	 */
	String encryptFilename(String cleartextName);

	/**
	 * Decrypts the name of a file.
	 * 
	 * @param ciphertextName A ciphertext filename without any preceeding directory paths.
	 * @return Decrypted filename.
	 * @throws DecryptFailedException If the decryption failed for various reasons (including wrong password).
	 */
	String decryptFilename(String ciphertextName) throws DecryptFailedException;

	/**
	 * @param metadataSupport Support object allowing the Cryptor to read and write its own metadata to the location of the encrypted file.
	 * @return Content length of the decrypted file or <code>null</code> if unknown.
	 * @throws MacAuthenticationFailedException If the MAC auth failed.
	 */
	Long decryptedContentLength(SeekableByteChannel encryptedFile) throws IOException, MacAuthenticationFailedException;

	/**
	 * @return Number of decrypted bytes. This might not be equal to the encrypted file size due to optional metadata written to it.
	 * @throws DecryptFailedException If decryption failed
	 */
	Long decryptFile(SeekableByteChannel encryptedFile, OutputStream plaintextFile) throws IOException, DecryptFailedException;

	/**
	 * @param pos First byte (inclusive)
	 * @param length Number of requested bytes beginning at pos.
	 * @return Number of decrypted bytes. This might not be equal to the number of bytes requested due to potential overheads.
	 * @throws DecryptFailedException If decryption failed
	 */
	Long decryptRange(SeekableByteChannel encryptedFile, OutputStream plaintextFile, long pos, long length) throws IOException, DecryptFailedException;

	/**
	 * @return Number of encrypted bytes. This might not be equal to the encrypted file size due to optional metadata written to it.
	 */
	Long encryptFile(InputStream plaintextFile, SeekableByteChannel encryptedFile) throws IOException, EncryptFailedException;

}

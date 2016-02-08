/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.crypto.engine;

/**
 * Provides deterministic encryption capabilities as filenames must not change on subsequent encryption attempts,
 * otherwise each change results in major directory structure changes which would be a terrible idea for cloud storage encryption.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Deterministic_encryption">Wikipedia on deterministic encryption</a>
 */
public interface FilenameCryptor {

	/**
	 * @return constant length string, that is unlikely to collide with any other name.
	 */
	String hashDirectoryId(String cleartextDirectoryId);

	/**
	 * Tests without an actual decryption attempt, if a name is a well-formed ciphertext.
	 * 
	 * @param ciphertextName Filename in question
	 * @return <code>true</code> if the given name is likely to be a valid ciphertext
	 */
	boolean isEncryptedFilename(String ciphertextName);

	/**
	 * @param cleartextName original filename including cleartext file extension
	 * @param associatedData optional associated data, that will not get encrypted but needs to be provided during decryption
	 * @return encrypted filename without any file extension
	 */
	String encryptFilename(String cleartextName, byte[]... associatedData);

	/**
	 * @param ciphertextName Ciphertext only, with any additional strings like file extensions stripped first.
	 * @param associatedData the same associated data used during encryption, otherwise and {@link AuthenticationFailedException} will be thrown
	 * @return cleartext filename, probably including its cleartext file extension.
	 */
	String decryptFilename(String ciphertextName, byte[]... associatedData) throws AuthenticationFailedException;
}

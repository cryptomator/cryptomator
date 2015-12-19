/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
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
	 * @param cleartextName original filename including cleartext file extension
	 * @return encrypted filename without any file extension
	 */
	String encryptFilename(String cleartextName);

	/**
	 * @param ciphertextName Ciphertext only, with any additional strings like file extensions stripped first.
	 * @return cleartext filename, probably including its cleartext file extension.
	 */
	String decryptFilename(String ciphertextName);
}

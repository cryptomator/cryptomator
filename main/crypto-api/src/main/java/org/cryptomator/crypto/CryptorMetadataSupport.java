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

/**
 * Methods that may be called by the Cryptor when accessing a path.
 */
public interface CryptorMetadataSupport {

	/**
	 * Persists encryptedMetadata in a metadata group.
	 * 
	 * @param metadataFilename File relative to
	 * @throws IOException
	 */
	void writeMetadata(String metadataGroup, byte[] encryptedMetadata) throws IOException;

	/**
	 * @return Previously written metadata stored in the given metadata group or <code>null</code> if no such group exists.
	 */
	byte[] readMetadata(String metadataGroup) throws IOException;

}
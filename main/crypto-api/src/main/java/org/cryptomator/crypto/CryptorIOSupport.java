package org.cryptomator.crypto;

import java.io.IOException;

/**
 * Methods that may be called by the Cryptor when accessing a path.
 */
public interface CryptorIOSupport {

	/**
	 * Persists encryptedMetadata to the given encryptedPath.
	 * 
	 * @param encryptedPath A relative path
	 * @throws IOException
	 */
	void writePathSpecificMetadata(String encryptedPath, byte[] encryptedMetadata) throws IOException;

	/**
	 * @return Previously written encryptedMetadata stored at the given encryptedPath or <code>null</code> if no such file exists.
	 */
	byte[] readPathSpecificMetadata(String encryptedPath) throws IOException;

}
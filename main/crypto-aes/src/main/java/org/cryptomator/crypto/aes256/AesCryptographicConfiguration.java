/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.aes256;

interface AesCryptographicConfiguration {

	/**
	 * Number of bytes used as seed for the PRNG.
	 */
	int PRNG_SEED_LENGTH = 16;

	/**
	 * Number of bytes of the master key. Should be the maximum possible AES key length to provide best security.
	 */
	int MASTER_KEY_LENGTH = 256;

	/**
	 * Number of bytes used as salt, where needed.
	 */
	int SALT_LENGTH = 8;

	/**
	 * 0-filled salt.
	 */
	byte[] EMPTY_SALT = new byte[SALT_LENGTH];

	/**
	 * Algorithm used for key derivation.
	 */
	String KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA1";

	/**
	 * Algorithm used for random number generation.
	 */
	String PRNG_ALGORITHM = "SHA1PRNG";

	/**
	 * Algorithm used for en/decryption.
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#AlgorithmParameters
	 */
	String CRYPTO_ALGORITHM = "AES";

	/**
	 * Cipher specs for masterkey encryption.
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher
	 */
	String MASTERKEY_CIPHER = "AES/CBC/PKCS5Padding";

	/**
	 * Cipher specs for file name encryption.
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher
	 */
	String FILE_NAME_CIPHER = "AES/CBC/PKCS5Padding";

	/**
	 * Cipher specs for content encryption. Using CTR-mode for random access.
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher
	 */
	String FILE_CONTENT_CIPHER = "AES/CTR/NoPadding";

	/**
	 * AES block size is 128 bit or 16 bytes.
	 */
	int AES_BLOCK_LENGTH = 16;

	/**
	 * 0-filled initialization vector.
	 */
	byte[] EMPTY_IV = new byte[AES_BLOCK_LENGTH];

	/**
	 * Number of iterations for key derived from user pw. High iteration count for better resistance to bruteforcing.
	 */
	int PBKDF2_PW_ITERATIONS = 1000;

	/**
	 * Number of iterations for key derived from masterkey. Low iteration count for better performance. No additional security is added by
	 * high values.
	 */
	int PBKDF2_MASTERKEY_ITERATIONS = 1;

}

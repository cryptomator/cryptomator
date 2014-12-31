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
	 * Algorithm used for key derivation as defined in RFC 2898 / PKCS #5.
	 * 
	 * SHA1 will deprecate soon, but the main purpose of PBKDF2 is waisting CPU cycles, so cryptographically strong hash algorithms are not
	 * necessary here. See also http://crypto.stackexchange.com/a/11017
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
	String AES_GCM_CIPHER = "AES/GCM/NoPadding";

	/**
	 * Length of authentication tag.
	 */
	int AES_GCM_TAG_LENGTH = 128;

	/**
	 * Cipher specs for file name and file content encryption. Using CTR-mode for random access.
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher
	 */
	String AES_CTR_CIPHER = "AES/CTR/NoPadding";

	/**
	 * AES block size is 128 bit or 16 bytes.
	 */
	int AES_BLOCK_LENGTH = 16;

	/**
	 * Number of non-zero bytes in the IV used for file name encryption. Less means shorter encrypted filenames, more means higher entropy.
	 * Maximum length is {@value #AES_BLOCK_LENGTH}. Even the shortest base32 (see {@link FileNamingConventions#ENCRYPTED_FILENAME_CODEC})
	 * encoded byte array will need 8 chars. The maximum number of bytes that fit in 8 base32 chars is 5. Thus 5 is the ideal length.
	 */
	int FILE_NAME_IV_LENGTH = 5;

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

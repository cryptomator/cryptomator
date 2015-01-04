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
	 * Number of bytes used as salt, where needed.
	 */
	int SCRYPT_SALT_LENGTH = 8;

	/**
	 * Scrypt CPU/Memory cost parameter.
	 */
	int SCRYPT_COST_PARAM = 1 << 14;

	/**
	 * Scrypt block size (affects memory consumption)
	 */
	int SCRYPT_BLOCK_SIZE = 8;

	/**
	 * Number of bytes of the master key. Should be the maximum possible AES key length to provide best security.
	 */
	int PREF_MASTER_KEY_LENGTH_IN_BITS = 256;

	/**
	 * Number of bytes used as seed for the PRNG.
	 */
	int PRNG_SEED_LENGTH = 16;

	/**
	 * Algorithm used for random number generation.
	 */
	String PRNG_ALGORITHM = "SHA1PRNG";

	/**
	 * Algorithm used for en/decryption.
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#AlgorithmParameters
	 */
	String AES_KEY_ALGORITHM = "AES";

	/**
	 * Key algorithm for keyed MAC.
	 */
	String HMAC_KEY_ALGORITHM = "HmacSHA256";

	/**
	 * Cipher specs for RFC 3394 masterkey encryption.
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher
	 */
	String AES_KEYWRAP_CIPHER = "AESWrap";

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

}

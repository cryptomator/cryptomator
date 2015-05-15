/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.aes256;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.BaseNCodec;

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
	 * Preferred number of bytes of the master key.
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
	 * Cipher specs for file content encryption. Using CTR-mode for random access.<br/>
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#Cipher
	 */
	String AES_CTR_CIPHER = "AES/CTR/NoPadding";

	/**
	 * Cipher specs for file header encryption (fixed-length block cipher).<br/>
	 * 
	 * @see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#impl
	 */
	String AES_CBC_CIPHER = "AES/CBC/PKCS5Padding";

	/**
	 * AES block size is 128 bit or 16 bytes.
	 */
	int AES_BLOCK_LENGTH = 16;

	/**
	 * How to encode the encrypted file names safely. Base32 uses only alphanumeric characters and is case-insensitive.
	 */
	BaseNCodec ENCRYPTED_FILENAME_CODEC = new Base32();

}

/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.aes256;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.BaseNCodec;

interface FileNamingConventions {

	/**
	 * Extension of masterkey files inside the root directory of the encrypted storage.
	 */
	String MASTERKEY_FILE_EXT = ".masterkey.json";

	/**
	 * How to encode the encrypted file names safely.
	 */
	BaseNCodec ENCRYPTED_FILENAME_CODEC = new Base32();

	/**
	 * Maximum length possible on file systems with a filename limit of 255 chars.<br/>
	 * 144 and 160 are multiples of 16 (128bit aes block size).<br/>
	 * 144 * 8/5 (base32) = 230,..<br/>
	 * 160 * 8/5 = 256<br/>
	 * Base 64 isn't supported on case-insensitive file systems.<br/>
	 */
	int PLAINTEXT_FILENAME_LENGTH_LIMIT = 144;

	/**
	 * For plaintext file names <= {@value #PLAINTEXT_FILENAME_LENGTH_LIMIT} chars.
	 */
	String BASIC_FILE_EXT = ".aes";

	/**
	 * For plaintext file names > {@value #PLAINTEXT_FILENAME_LENGTH_LIMIT} chars.
	 */
	String LONG_NAME_FILE_EXT = ".lng.aes";

	/**
	 * Matches both, {@value #BASIC_FILE_EXT} and {@value #LONG_NAME_FILE_EXT} files.
	 */
	PathMatcher ENCRYPTED_FILE_GLOB_MATCHER = FileSystems.getDefault().getPathMatcher("glob:**/*{" + BASIC_FILE_EXT + "," + LONG_NAME_FILE_EXT + "}");

}

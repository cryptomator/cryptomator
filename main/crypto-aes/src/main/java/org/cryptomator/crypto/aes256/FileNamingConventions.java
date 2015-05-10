/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.aes256;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.BaseNCodec;
import org.apache.commons.lang3.StringUtils;

interface FileNamingConventions {

	/**
	 * How to encode the encrypted file names safely. Base32 uses only alphanumeric characters and is case-insensitive.
	 */
	BaseNCodec ENCRYPTED_FILENAME_CODEC = new Base32();

	/**
	 * Maximum path length on some file systems or cloud storage providers is restricted.<br/>
	 * Parent folder path uses up to 58 chars (sha256 -&gt; 32 bytes base32 encoded to 56 bytes + two slashes). That in mind we don't want the total path to be longer than 255 chars.<br/>
	 * 128 chars would be enought for up to 80 plaintext chars. Also we need up to 8 chars for our file extension. So lets use {@value #ENCRYPTED_FILENAME_LENGTH_LIMIT}.
	 */
	int ENCRYPTED_FILENAME_LENGTH_LIMIT = 136;

	/**
	 * For plaintext file names <= {@value #ENCRYPTED_FILENAME_LENGTH_LIMIT} chars.
	 */
	String BASIC_FILE_EXT = ".aes";

	/**
	 * For plaintext file names > {@value #ENCRYPTED_FILENAME_LENGTH_LIMIT} chars.
	 */
	String LONG_NAME_FILE_EXT = ".lng.aes";

	/**
	 * Length of prefix in file names > {@value #ENCRYPTED_FILENAME_LENGTH_LIMIT} chars used to determine the corresponding metadata file.
	 */
	int LONG_NAME_PREFIX_LENGTH = 8;

	/**
	 * Matches valid encrypted filenames (both normal and long filenames - see {@link #ENCRYPTED_FILENAME_LENGTH_LIMIT}).
	 */
	PathMatcher ENCRYPTED_FILE_MATCHER = new PathMatcher() {

		private final Pattern BASIC_NAME_PATTERN = Pattern.compile("^[a-z2-7]+=*$", Pattern.CASE_INSENSITIVE);
		private final Pattern LONG_NAME_PATTERN = Pattern.compile("^[a-z2-7]{8}[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);

		@Override
		public boolean matches(Path path) {
			final String filename = path.getFileName().toString();
			if (StringUtils.endsWithIgnoreCase(filename, LONG_NAME_FILE_EXT)) {
				final String basename = StringUtils.removeEndIgnoreCase(filename, LONG_NAME_FILE_EXT);
				return LONG_NAME_PATTERN.matcher(basename).matches();
			} else if (StringUtils.endsWithIgnoreCase(filename, BASIC_FILE_EXT)) {
				final String basename = StringUtils.removeEndIgnoreCase(filename, BASIC_FILE_EXT);
				return BASIC_NAME_PATTERN.matcher(basename).matches();
			} else {
				return false;
			}
		}

	};

}

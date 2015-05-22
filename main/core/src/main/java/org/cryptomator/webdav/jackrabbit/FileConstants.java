/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

interface FileConstants {

	/**
	 * Number of bytes in the file header.
	 */
	long FILE_HEADER_LENGTH = 96;

	/**
	 * Allow range requests for files > 32MiB.
	 */
	long RANGE_REQUEST_LOWER_LIMIT = 32 * 1024 * 1024;

	/**
	 * Maximum path length on some file systems or cloud storage providers is restricted.<br/>
	 * Parent folder path uses up to 58 chars (sha256 -&gt; 32 bytes base32 encoded to 56 bytes + two slashes). That in mind we don't want the total path to be longer than 255 chars.<br/>
	 * 128 chars would be enought for up to 80 plaintext chars. Also we need up to 9 chars for our file extension. So lets use {@value #ENCRYPTED_FILENAME_LENGTH_LIMIT}.
	 */
	int ENCRYPTED_FILENAME_LENGTH_LIMIT = 137;

	/**
	 * Dummy file, on which file attributes can be stored for the root directory.
	 */
	String ROOT_FILE = "root";

	/**
	 * For encrypted directory names <= {@value #ENCRYPTED_FILENAME_LENGTH_LIMIT} chars.
	 */
	String DIR_EXT = ".dir";

	/**
	 * For encrypted direcotry names > {@value #ENCRYPTED_FILENAME_LENGTH_LIMIT} chars.
	 */
	String LONG_DIR_EXT = ".lng.dir";

	/**
	 * For encrypted file names <= {@value #ENCRYPTED_FILENAME_LENGTH_LIMIT} chars.
	 */
	String FILE_EXT = ".file";

	/**
	 * For encrypted file names > {@value #ENCRYPTED_FILENAME_LENGTH_LIMIT} chars.
	 */
	String LONG_FILE_EXT = ".lng.file";

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
			if (StringUtils.endsWithIgnoreCase(filename, LONG_FILE_EXT)) {
				final String basename = StringUtils.removeEndIgnoreCase(filename, LONG_FILE_EXT);
				return LONG_NAME_PATTERN.matcher(basename).matches();
			} else if (StringUtils.endsWithIgnoreCase(filename, FILE_EXT)) {
				final String basename = StringUtils.removeEndIgnoreCase(filename, FILE_EXT);
				return BASIC_NAME_PATTERN.matcher(basename).matches();
			} else if (StringUtils.endsWithIgnoreCase(filename, LONG_DIR_EXT)) {
				final String basename = StringUtils.removeEndIgnoreCase(filename, LONG_DIR_EXT);
				return LONG_NAME_PATTERN.matcher(basename).matches();
			} else if (StringUtils.endsWithIgnoreCase(filename, DIR_EXT)) {
				final String basename = StringUtils.removeEndIgnoreCase(filename, DIR_EXT);
				return BASIC_NAME_PATTERN.matcher(basename).matches();
			} else {
				return false;
			}
		}

	};

	/**
	 * Filter to determine files of interest in encrypted directory. Based on {@link #ENCRYPTED_FILE_MATCHER}.
	 */
	Filter<Path> DIRECTORY_CONTENT_FILTER = new Filter<Path>() {
		@Override
		public boolean accept(Path entry) throws IOException {
			return ENCRYPTED_FILE_MATCHER.matches(entry);
		}
	};

}

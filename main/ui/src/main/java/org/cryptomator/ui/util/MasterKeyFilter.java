/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.cryptomator.crypto.aes256.Aes256Cryptor;

public class MasterKeyFilter implements Filter<Path> {

	public static MasterKeyFilter FILTER = new MasterKeyFilter();

	private final String masterKeyExt = Aes256Cryptor.MASTERKEY_FILE_EXT.toLowerCase();

	@Override
	public boolean accept(Path child) throws IOException {
		return child.getFileName().toString().toLowerCase().endsWith(masterKeyExt);
	}

	public static final DirectoryStream<Path> filteredDirectory(Path dir) throws IOException {
		return Files.newDirectoryStream(dir, FILTER);
	}

}

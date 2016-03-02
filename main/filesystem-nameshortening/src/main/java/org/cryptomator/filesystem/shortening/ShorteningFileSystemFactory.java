/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.shortening;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

@Singleton
public class ShorteningFileSystemFactory {

	private static final int SHORTENING_THRESHOLD = 129; // 128 + "_"
	private static final String METADATA_FOLDER_NAME = "m";

	@Inject
	public ShorteningFileSystemFactory() {
	}

	public FileSystem get(Folder root) {
		return new ShorteningFileSystem(root, METADATA_FOLDER_NAME, SHORTENING_THRESHOLD);
	}
}

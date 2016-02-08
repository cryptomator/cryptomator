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
import org.cryptomator.filesystem.blacklisting.BlacklistingFileSystemFactory;
import org.cryptomator.filesystem.blacklisting.SamePathPredicate;

@Singleton
public class ShorteningFileSystemFactory {

	private static final int SHORTENING_THRESHOLD = 129; // 128 + "_"
	private static final String METADATA_FOLDER_NAME = "m";

	private final BlacklistingFileSystemFactory blacklistingFileSystemFactory;

	@Inject
	public ShorteningFileSystemFactory(BlacklistingFileSystemFactory blacklistingFileSystemFactory) {
		this.blacklistingFileSystemFactory = blacklistingFileSystemFactory;
	}

	public FileSystem get(Folder root) {
		final Folder metadataFolder = root.folder(METADATA_FOLDER_NAME);
		final FileSystem metadataHidingFs = blacklistingFileSystemFactory.get(root, SamePathPredicate.forNode(metadataFolder));
		return new ShorteningFileSystem(metadataHidingFs, metadataFolder, SHORTENING_THRESHOLD);
	}
}

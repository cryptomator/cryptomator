/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.blockaligned;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.crypto.engine.impl.FileContentCryptorImpl;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

@Singleton
public class BlockAlignedFileSystemFactory {

	@Inject
	public BlockAlignedFileSystemFactory() {
	}

	public FileSystem get(Folder root) {
		return new BlockAlignedFileSystem(root, FileContentCryptorImpl.CHUNK_SIZE);
	}
}

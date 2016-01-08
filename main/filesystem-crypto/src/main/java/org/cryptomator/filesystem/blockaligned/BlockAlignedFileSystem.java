/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.blockaligned;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.FileSystemFeature;
import org.cryptomator.filesystem.Folder;

class BlockAlignedFileSystem extends BlockAlignedFolder implements FileSystem {

	public BlockAlignedFileSystem(Folder delegate, int blockSize) {
		super(null, delegate, blockSize);
	}

	@Override
	public boolean supports(FileSystemFeature feature) {
		return delegate.fileSystem().supports(feature);
	}

}

/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.delegating.DelegatingFolder;

class BlockAlignedFolder extends DelegatingFolder<BlockAlignedFolder, BlockAlignedFile> {

	private final int blockSize;

	public BlockAlignedFolder(BlockAlignedFolder parent, Folder delegate, int blockSize) {
		super(parent, delegate);
		this.blockSize = blockSize;
	}

	@Override
	protected BlockAlignedFile newFile(File delegate) {
		return new BlockAlignedFile(this, delegate, blockSize);
	}

	@Override
	protected BlockAlignedFolder newFolder(Folder delegate) {
		return new BlockAlignedFolder(this, delegate, blockSize);
	}

}

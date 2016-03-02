/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.crypto;

import java.io.UncheckedIOException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.delegating.DelegatingFile;

class BlockAlignedFile extends DelegatingFile<BlockAlignedFolder> {

	private final int blockSize;

	public BlockAlignedFile(BlockAlignedFolder parent, File delegate, int blockSize) {
		super(parent, delegate);
		this.blockSize = blockSize;
	}

	@Override
	public BlockAlignedReadableFile openReadable() throws UncheckedIOException {
		return new BlockAlignedReadableFile(delegate.openReadable(), blockSize);
	}

	@Override
	public BlockAlignedWritableFile openWritable() throws UncheckedIOException {
		return new BlockAlignedWritableFile(delegate::openWritable, delegate::openReadable, blockSize);
	}

}

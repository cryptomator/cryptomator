package org.cryptomator.filesystem.blockaligned;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.delegating.DelegatingFolder;

class BlockAlignedFolder extends DelegatingFolder<BlockAlignedReadableFile, BlockAlignedWritableFile, BlockAlignedFolder, BlockAlignedFile> {

	private final int blockSize;

	public BlockAlignedFolder(BlockAlignedFolder parent, Folder delegate, int blockSize) {
		super(parent, delegate);
		this.blockSize = blockSize;
	}

	@Override
	protected BlockAlignedFile file(File delegate) {
		return new BlockAlignedFile(this, delegate, blockSize);
	}

	@Override
	protected BlockAlignedFolder folder(Folder delegate) {
		return new BlockAlignedFolder(this, delegate, blockSize);
	}

}

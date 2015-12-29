package org.cryptomator.filesystem.blockaligned;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

public class BlockAlignedFileSystem extends BlockAlignedFolder implements FileSystem {

	public BlockAlignedFileSystem(Folder delegate, int blockSize) {
		super(null, delegate, blockSize);
	}

}

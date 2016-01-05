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

package org.cryptomator.filesystem.nio;

import static org.cryptomator.filesystem.FolderCreateMode.INCLUDING_PARENTS;

import java.nio.file.Path;
import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;

public class NioFileSystem extends NioFolder implements FileSystem {

	public static NioFileSystem rootedAt(Path root) {
		return new NioFileSystem(root);
	}

	private NioFileSystem(Path root) {
		super(Optional.empty(), root);
		create(INCLUDING_PARENTS);
	}

}

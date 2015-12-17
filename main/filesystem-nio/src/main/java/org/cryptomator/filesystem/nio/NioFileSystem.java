package org.cryptomator.filesystem.nio;

import java.nio.file.Path;
import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;

public class NioFileSystem extends NioFolder implements FileSystem {

	public static NioFileSystem rootedAt(Path root) {
		return new NioFileSystem(root, new DefaultNioNodeFactory());
	}

	NioFileSystem(Path root, NioNodeFactory nodeFactory) {
		super(Optional.empty(), root, nodeFactory);
	}

}

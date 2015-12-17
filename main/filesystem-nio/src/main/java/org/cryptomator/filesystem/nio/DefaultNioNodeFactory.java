package org.cryptomator.filesystem.nio;

import java.nio.file.Path;
import java.util.Optional;

class DefaultNioNodeFactory implements NioNodeFactory {

	@Override
	public NioFile file(Optional<NioFolder> parent, Path path) {
		return new NioFile(parent, path, this);
	}

	@Override
	public NioFolder folder(Optional<NioFolder> parent, Path path) {
		return new NioFolder(parent, path, this);
	}

}

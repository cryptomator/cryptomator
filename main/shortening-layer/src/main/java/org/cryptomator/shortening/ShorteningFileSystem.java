package org.cryptomator.shortening;

import java.util.Optional;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

public class ShorteningFileSystem extends ShorteningFolder implements FileSystem {

	public ShorteningFileSystem(Folder root, Folder metadataRoot, int threshold) {
		super(null, root, "", metadataRoot, new FilenameShortener(metadataRoot, threshold));
	}

	@Override
	public Optional<ShorteningFolder> parent() {
		return Optional.empty();
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public void delete() {
		// no-op.
	}

}

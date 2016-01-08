package org.cryptomator.filesystem.shortening;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.FileSystemFeature;
import org.cryptomator.filesystem.Folder;

class ShorteningFileSystem extends ShorteningFolder implements FileSystem {

	public ShorteningFileSystem(Folder root, Folder metadataRoot, int threshold) {
		super(null, root, "", new FilenameShortener(metadataRoot, threshold));
	}

	@Override
	public boolean supports(FileSystemFeature feature) {
		return delegate.fileSystem().supports(feature);
	}

}

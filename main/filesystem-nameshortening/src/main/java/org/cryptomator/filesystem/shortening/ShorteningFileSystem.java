package org.cryptomator.filesystem.shortening;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

public class ShorteningFileSystem extends ShorteningFolder implements FileSystem {

	public ShorteningFileSystem(Folder root, Folder metadataRoot, int threshold) {
		super(null, root, "", new FilenameShortener(metadataRoot, threshold));
		create();
	}

}

package org.cryptomator.shortening;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

/**
 * Filesystem implementation, that shortens filenames when they reach a certain
 * threshold (inclusive). Shortening is done by SHA1-hashing those files, so a
 * threshold below the length of the hashed files makes no sense. Hashes are
 * then mapped back to the original filenames by storing metadata files inside
 * the given metadataRoot.
 */
public class ShorteningFileSystem extends ShorteningFolder implements FileSystem {

	public ShorteningFileSystem(Folder root, Folder metadataRoot, int threshold) {
		super(null, root, "", metadataRoot, new FilenameShortener(metadataRoot, threshold));
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public void delete() {
		// no-op.
	}

	@Override
	public String toString() {
		return "/";
	}

}

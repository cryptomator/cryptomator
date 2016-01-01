package org.cryptomator.filesystem.jackrabbit;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

class FileSystemLocator extends FolderLocator implements FileSystem {

	public FileSystemLocator(DavLocatorFactory factory, String prefix, Folder delegate) {
		super(factory, prefix, null, delegate);
	}

	@Override
	public boolean isRootLocation() {
		return true;
	}

	@Override
	public String getResourcePath() {
		return "/";
	}

}

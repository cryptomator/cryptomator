package org.cryptomator.filesystem.delegating;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;

class TestDelegatingFileSystem extends TestDelegatingFolder implements FileSystem {

	private TestDelegatingFileSystem(Folder delegate) {
		super(null, delegate);
	}

	public static TestDelegatingFileSystem withRoot(Folder delegate) {
		return new TestDelegatingFileSystem(delegate);
	}

}

package org.cryptomator.filesystem.delegating;

import org.cryptomator.filesystem.Folder;

class TestDelegatingFileSystem extends TestDelegatingFolder implements DelegatingFileSystem {

	private TestDelegatingFileSystem(Folder delegate) {
		super(null, delegate);
	}

	public static TestDelegatingFileSystem withRoot(Folder delegate) {
		return new TestDelegatingFileSystem(delegate);
	}

	@Override
	public Folder getDelegate() {
		return delegate;
	}

}

package org.cryptomator.filesystem.delegating;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;

class TestDelegatingFolder extends DelegatingFolder<TestDelegatingFolder, TestDelegatingFile> {

	public TestDelegatingFolder(TestDelegatingFolder parent, Folder delegate) {
		super(parent, delegate);
	}

	@Override
	protected TestDelegatingFile newFile(File delegate) {
		return new TestDelegatingFile(this, delegate);
	}

	@Override
	protected TestDelegatingFolder newFolder(Folder delegate) {
		return new TestDelegatingFolder(this, delegate);
	}

}

package org.cryptomator.filesystem.delegating;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;

class TestDelegatingFolder extends DelegatingFolder<DelegatingReadableFile, DelegatingWritableFile, TestDelegatingFolder, TestDelegatingFile> {

	public TestDelegatingFolder(TestDelegatingFolder parent, Folder delegate) {
		super(parent, delegate);
	}

	@Override
	protected TestDelegatingFile file(File delegate) {
		return new TestDelegatingFile(this, delegate);
	}

	@Override
	protected TestDelegatingFolder folder(Folder delegate) {
		return new TestDelegatingFolder(this, delegate);
	}

}

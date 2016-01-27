package org.cryptomator.filesystem.delegating;

import org.cryptomator.filesystem.File;

class TestDelegatingFile extends DelegatingFile<TestDelegatingFolder> {

	public TestDelegatingFile(TestDelegatingFolder parent, File delegate) {
		super(parent, delegate);
	}

}

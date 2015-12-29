package org.cryptomator.filesystem.delegating;

import java.io.UncheckedIOException;

import org.cryptomator.filesystem.File;

class TestDelegatingFile extends DelegatingFile<DelegatingReadableFile, DelegatingWritableFile, TestDelegatingFolder> {

	public TestDelegatingFile(TestDelegatingFolder parent, File delegate) {
		super(parent, delegate);
	}

	@Override
	public DelegatingReadableFile openReadable() throws UncheckedIOException {
		return new DelegatingReadableFile(delegate.openReadable());
	}

	@Override
	public DelegatingWritableFile openWritable() throws UncheckedIOException {
		return new DelegatingWritableFile(delegate.openWritable());
	}

}

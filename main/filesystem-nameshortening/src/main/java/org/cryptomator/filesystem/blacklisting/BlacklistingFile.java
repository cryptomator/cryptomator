package org.cryptomator.filesystem.blacklisting;

import java.io.UncheckedIOException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.delegating.DelegatingFile;
import org.cryptomator.filesystem.delegating.DelegatingReadableFile;
import org.cryptomator.filesystem.delegating.DelegatingWritableFile;

class BlacklistingFile extends DelegatingFile<DelegatingReadableFile, DelegatingWritableFile, BlacklistingFolder> {

	public BlacklistingFile(BlacklistingFolder parent, File delegate) {
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

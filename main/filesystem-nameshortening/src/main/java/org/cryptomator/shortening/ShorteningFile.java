package org.cryptomator.shortening;

import java.io.UncheckedIOException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class ShorteningFile extends ShorteningNode<File> implements File {

	private final FilenameShortener shortener;

	public ShorteningFile(ShorteningFolder parent, File delegate, String longName, FilenameShortener shortener) {
		super(parent, delegate, longName);
		this.shortener = shortener;
	}

	@Override
	public ReadableFile openReadable() throws UncheckedIOException {
		return delegate.openReadable();
	}

	@Override
	public WritableFile openWritable() throws UncheckedIOException {
		if (shortener.isShortened(shortName())) {
			shortener.saveMapping(name(), shortName());
		}
		return delegate.openWritable();
	}

	@Override
	public String toString() {
		return parent + name();
	}

	@Override
	public int compareTo(File o) {
		return toString().compareTo(o.toString());
	}

}

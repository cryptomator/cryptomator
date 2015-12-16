package org.cryptomator.shortening;

import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class ShorteningFile extends ShorteningNode<File>implements File {

	private final FilenameShortener shortener;

	public ShorteningFile(ShorteningFolder parent, File delegate, String longName, FilenameShortener shortener) {
		super(parent, delegate, longName);
		this.shortener = shortener;
	}

	@Override
	public ReadableFile openReadable(long timeout, TimeUnit unit) throws UncheckedIOException, TimeoutException {
		return delegate.openReadable(timeout, unit);
	}

	@Override
	public WritableFile openWritable(long timeout, TimeUnit unit) throws UncheckedIOException, TimeoutException {
		if (shortener.isShortened(shortName())) {
			shortener.saveMapping(name(), shortName());
		}
		return delegate.openWritable(timeout, unit);
	}

	@Override
	public String toString() {
		return name();
	}

}

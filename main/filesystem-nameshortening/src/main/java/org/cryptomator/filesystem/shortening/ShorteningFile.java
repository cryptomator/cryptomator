package org.cryptomator.filesystem.shortening;

import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;

import org.cryptomator.common.LazyInitializer;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.delegating.DelegatingFile;
import org.cryptomator.filesystem.delegating.DelegatingReadableFile;
import org.cryptomator.filesystem.delegating.DelegatingWritableFile;

class ShorteningFile extends DelegatingFile<DelegatingReadableFile, DelegatingWritableFile, ShorteningFolder> {

	private final AtomicReference<String> longName;
	private final FilenameShortener shortener;

	public ShorteningFile(ShorteningFolder parent, File delegate, String name, FilenameShortener shortener) {
		super(parent, delegate);
		this.longName = new AtomicReference<>(name);
		this.shortener = shortener;
	}

	@Override
	public String name() throws UncheckedIOException {
		return LazyInitializer.initializeLazily(longName, () -> {
			return shortener.inflate(shortenedName());
		});
	}

	private String shortenedName() {
		return delegate.name();
	}

	@Override
	public DelegatingReadableFile openReadable() throws UncheckedIOException {
		return new DelegatingReadableFile(delegate.openReadable());
	}

	@Override
	public DelegatingWritableFile openWritable() throws UncheckedIOException {
		if (shortener.isShortened(shortenedName())) {
			shortener.saveMapping(name(), shortenedName());
		}
		return new DelegatingWritableFile(delegate.openWritable());
	}

}

package org.cryptomator.filesystem.shortening;

import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;

import org.cryptomator.common.LazyInitializer;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.delegating.DelegatingFolder;
import org.cryptomator.filesystem.delegating.DelegatingReadableFile;
import org.cryptomator.filesystem.delegating.DelegatingWritableFile;

class ShorteningFolder extends DelegatingFolder<DelegatingReadableFile, DelegatingWritableFile, ShorteningFolder, ShorteningFile> {

	private final AtomicReference<String> longName;
	private final FilenameShortener shortener;

	public ShorteningFolder(ShorteningFolder parent, Folder delegate, String name, FilenameShortener shortener) {
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
	public ShorteningFile file(String name) throws UncheckedIOException {
		return new ShorteningFile(this, delegate.file(shortener.deflate(name)), name, shortener);
	}

	@Override
	public ShorteningFolder folder(String name) throws UncheckedIOException {
		return new ShorteningFolder(this, delegate.folder(shortener.deflate(name)), name, shortener);
	}

	@Override
	protected ShorteningFile newFile(File delegate) {
		return new ShorteningFile(this, delegate, null, shortener);
	}

	@Override
	protected ShorteningFolder newFolder(Folder delegate) {
		return new ShorteningFolder(this, delegate, null, shortener);
	}

	@Override
	public void create() throws UncheckedIOException {
		if (exists()) {
			return;
		}
		parent().get().create();
		if (shortener.isShortened(shortenedName())) {
			shortener.saveMapping(name(), shortenedName());
		}
		super.create();
	}

	@Override
	public void moveTo(Folder destination) {
		super.moveTo(destination);
		if (destination instanceof ShorteningFolder) {
			ShorteningFolder dest = (ShorteningFolder) destination;
			if (shortener.isShortened(dest.shortenedName())) {
				shortener.saveMapping(dest.name(), dest.shortenedName());
			}
		}
	}

}

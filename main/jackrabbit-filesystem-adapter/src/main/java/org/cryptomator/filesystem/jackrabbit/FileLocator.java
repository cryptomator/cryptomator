package org.cryptomator.filesystem.jackrabbit;

import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.delegating.DelegatingFile;
import org.cryptomator.filesystem.delegating.DelegatingReadableFile;
import org.cryptomator.filesystem.delegating.DelegatingWritableFile;

public class FileLocator extends DelegatingFile<DelegatingReadableFile, DelegatingWritableFile, FolderLocator>implements FileSystemResourceLocator {

	private final DavLocatorFactory factory;
	private final String prefix;
	private final AtomicReference<String> resourcePath = new AtomicReference<>();

	public FileLocator(DavLocatorFactory factory, String prefix, FolderLocator parent, File delegate) {
		super(parent, delegate);
		this.factory = factory;
		this.prefix = prefix;
	}

	@Override
	public DelegatingReadableFile openReadable() throws UncheckedIOException {
		return new DelegatingReadableFile(delegate.openReadable());
	}

	@Override
	public DelegatingWritableFile openWritable() throws UncheckedIOException {
		return new DelegatingWritableFile(delegate.openWritable());
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	@Override
	public boolean isRootLocation() {
		return false;
	}

	@Override
	public DavLocatorFactory getFactory() {
		return factory;
	}

	@Override
	public AtomicReference<String> getResourcePathRef() {
		return resourcePath;
	}

	@Override
	public String computeResourcePath() {
		assert parent().isPresent();
		return parent().get().getResourcePath() + name();
	}

}

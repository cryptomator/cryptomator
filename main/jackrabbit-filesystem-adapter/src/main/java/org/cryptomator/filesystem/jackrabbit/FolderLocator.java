package org.cryptomator.filesystem.jackrabbit;

import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.delegating.DelegatingFolder;
import org.cryptomator.filesystem.delegating.DelegatingReadableFile;
import org.cryptomator.filesystem.delegating.DelegatingWritableFile;

public class FolderLocator extends DelegatingFolder<DelegatingReadableFile, DelegatingWritableFile, FolderLocator, FileLocator>implements FileSystemResourceLocator {

	private final DavLocatorFactory factory;
	private final String prefix;
	private final AtomicReference<String> resourcePath = new AtomicReference<>();

	public FolderLocator(DavLocatorFactory factory, String prefix, FolderLocator parent, Folder delegate) {
		super(parent, delegate);
		this.factory = factory;
		this.prefix = prefix;
	}

	@Override
	protected FileLocator file(File delegate) {
		return new FileLocator(factory, prefix, this, delegate);
	}

	@Override
	public FileLocator resolveFile(String relativePath) throws UncheckedIOException {
		return (FileLocator) super.resolveFile(relativePath);
	}

	@Override
	protected FolderLocator folder(Folder delegate) {
		return new FolderLocator(factory, prefix, this, delegate);
	}

	@Override
	public FolderLocator resolveFolder(String relativePath) throws UncheckedIOException {
		return (FolderLocator) super.resolveFolder(relativePath);
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
		return parent().get().getResourcePath() + name() + "/";
	}

}

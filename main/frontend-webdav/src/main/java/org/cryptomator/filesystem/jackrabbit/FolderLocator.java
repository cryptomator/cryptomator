/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.jackrabbit;

import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.delegating.DelegatingFolder;

public class FolderLocator extends DelegatingFolder<FolderLocator, FileLocator>implements InternalFileSystemResourceLocator {

	private final DavLocatorFactory factory;
	private final String prefix;
	private final AtomicReference<String> resourcePath = new AtomicReference<>();

	public FolderLocator(DavLocatorFactory factory, String prefix, FolderLocator parent, Folder delegate) {
		super(parent, delegate);
		this.factory = factory;
		this.prefix = prefix;
	}

	@Override
	protected FileLocator newFile(File delegate) {
		return new FileLocator(factory, prefix, this, delegate);
	}

	@Override
	public FileLocator resolveFile(String relativePath) throws UncheckedIOException {
		return (FileLocator) super.resolveFile(relativePath);
	}

	@Override
	protected FolderLocator newFolder(Folder delegate) {
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

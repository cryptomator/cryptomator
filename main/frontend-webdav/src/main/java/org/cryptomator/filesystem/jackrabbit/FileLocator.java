/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.jackrabbit;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.delegating.DelegatingFile;

public class FileLocator extends DelegatingFile<FolderLocator>implements InternalFileSystemResourceLocator {

	private final DavLocatorFactory factory;
	private final String prefix;
	private final AtomicReference<String> resourcePath = new AtomicReference<>();

	public FileLocator(DavLocatorFactory factory, String prefix, FolderLocator parent, File delegate) {
		super(parent, delegate);
		this.factory = factory;
		this.prefix = prefix;
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

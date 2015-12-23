/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

class FilesystemResourceFactory implements DavResourceFactory {

	private static final Class<Folder> FOLDER = Folder.class;
	private static final Class<File> FILE = File.class;

	private final FileSystem filesystem;
	private final LockManager lockManager;

	public FilesystemResourceFactory(FileSystem filesystem) {
		this.filesystem = filesystem;
		this.lockManager = new SimpleLockManager();
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		return createResource(locator, request.getDavSession());
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		final String path = locator.getResourcePath();
		if (path.endsWith("/")) {
			Folder folder = this.resolve(path, FOLDER);
			return createFolder(folder, locator, session);
		} else {
			File file = this.resolve(path, FILE);
			return createFile(file, locator, session);
		}
	}

	DavFolder createFolder(Folder folder, DavResourceLocator locator, DavSession session) {
		return new DavFolder(this, lockManager, session, locator, folder);
	}

	public DavFile createFile(File file, DavResourceLocator locator, DavSession session) {
		return new DavFile(this, lockManager, session, locator, file);
	}

	private <T extends Node> T resolve(String path, Class<T> expectedNodeType) {
		final String[] pathFragments = StringUtils.split(path, '/');
		if (ArrayUtils.isEmpty(pathFragments)) {
			assert expectedNodeType.isAssignableFrom(Folder.class);
			return expectedNodeType.cast(filesystem);
		} else {
			return resolve(filesystem, Arrays.stream(pathFragments).iterator(), expectedNodeType);
		}
	}

	private <T extends Node> T resolve(Folder parent, Iterator<String> pathIterator, Class<T> expectedNodeType) {
		assert pathIterator.hasNext();
		final String childName = pathIterator.next();
		if (pathIterator.hasNext()) {
			return resolve(parent.folder(childName), pathIterator, expectedNodeType);
		} else if (expectedNodeType.isAssignableFrom(Folder.class)) {
			return expectedNodeType.cast(parent.folder(childName));
		} else if (expectedNodeType.isAssignableFrom(File.class)) {
			return expectedNodeType.cast(parent.file(childName));
		} else {
			throw new IllegalArgumentException("Supported expectedNodeTypes are File or Folder.");
		}
	}

}

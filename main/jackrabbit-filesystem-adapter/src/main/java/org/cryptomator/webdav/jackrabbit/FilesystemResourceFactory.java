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
import org.cryptomator.webdav.jackrabbit.DavPathFactory.DavPath;

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
		if (locator instanceof DavPath) {
			return createResource((DavPath) locator, session);
		} else {
			throw new IllegalArgumentException("Unsupported locator type " + locator.getClass().getName());
		}
	}

	private DavResource createResource(DavPath path, DavSession session) throws DavException {
		if (path.isDirectory()) {
			Folder folder = this.resolve(path.getResourcePath(), FOLDER);
			return createFolder(folder, path, session);
		} else {
			File file = this.resolve(path.getResourcePath(), FILE);
			return createFile(file, path, session);
		}
	}

	DavFolder createFolder(Folder folder, DavPath path, DavSession session) {
		return new DavFolder(this, lockManager, session, path, folder);
	}

	DavFile createFile(File file, DavPath path, DavSession session) {
		return new DavFile(this, lockManager, session, path, file);
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

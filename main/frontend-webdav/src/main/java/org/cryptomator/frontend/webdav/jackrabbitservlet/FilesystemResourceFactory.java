/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.cryptomator.filesystem.jackrabbit.FileLocator;
import org.cryptomator.filesystem.jackrabbit.FolderLocator;

class FilesystemResourceFactory implements DavResourceFactory {

	private final LockManager lockManager;

	public FilesystemResourceFactory() {
		this.lockManager = new ExclusiveSharedLockManager();
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		return createResource(locator, request.getDavSession());
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		if (locator instanceof FolderLocator) {
			FolderLocator folder = (FolderLocator) locator;
			return createFolder(folder, session);
		} else if (locator instanceof FileLocator) {
			FileLocator file = (FileLocator) locator;
			return createFile(file, session);
		} else {
			throw new IllegalArgumentException("Unsupported locator type " + locator.getClass().getName());
		}
	}

	DavFolder createFolder(FolderLocator folder, DavSession session) {
		return new DavFolder(this, lockManager, session, folder);
	}

	DavFile createFile(FileLocator file, DavSession session) {
		return new DavFile(this, lockManager, session, file);
	}

}

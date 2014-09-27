/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.webdav.jackrabbit;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;

import de.sebastianstenzel.oce.crypto.Cryptor;
import de.sebastianstenzel.oce.webdav.jackrabbit.resources.EncryptedDir;
import de.sebastianstenzel.oce.webdav.jackrabbit.resources.EncryptedFile;
import de.sebastianstenzel.oce.webdav.jackrabbit.resources.NonExistingNode;
import de.sebastianstenzel.oce.webdav.jackrabbit.resources.PathUtils;

public class WebDavResourceFactory implements DavResourceFactory {

	private final LockManager lockManager = new SimpleLockManager();
	private final Cryptor cryptor;

	public WebDavResourceFactory(Cryptor cryptor) {
		this.cryptor = cryptor;
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		final Path path = PathUtils.getPhysicalPath(locator);

		if (Files.exists(path)) {
			return createResource(locator, request.getDavSession());
		} else if (DavMethods.METHOD_MKCOL.equals(request.getMethod())) {
			return createDirectory(locator, request.getDavSession());
		} else if (DavMethods.METHOD_PUT.equals(request.getMethod())) {
			return createFile(locator, request.getDavSession());
		} else {
			return createNonExisting(locator, request.getDavSession());
		}
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		final Path path = PathUtils.getPhysicalPath(locator);

		if (Files.isDirectory(path)) {
			return createDirectory(locator, session);
		} else if (Files.isRegularFile(path)) {
			return createFile(locator, session);
		} else {
			return createNonExisting(locator, session);
		}
	}

	private EncryptedFile createFile(DavResourceLocator locator, DavSession session) {
		return new EncryptedFile(this, locator, session, lockManager, cryptor);
	}

	private EncryptedDir createDirectory(DavResourceLocator locator, DavSession session) {
		return new EncryptedDir(this, locator, session, lockManager, cryptor);
	}

	private NonExistingNode createNonExisting(DavResourceLocator locator, DavSession session) {
		return new NonExistingNode(this, locator, session, lockManager, cryptor);
	}

}

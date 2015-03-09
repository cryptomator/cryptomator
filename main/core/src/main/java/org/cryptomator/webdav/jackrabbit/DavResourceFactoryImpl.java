/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.httpclient.HttpStatus;
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
import org.cryptomator.crypto.Cryptor;
import org.eclipse.jetty.http.HttpHeader;

class DavResourceFactoryImpl implements DavResourceFactory {

	private final LockManager lockManager = new SimpleLockManager();
	private final Cryptor cryptor;
	private final CryptoWarningHandler cryptoWarningHandler;

	DavResourceFactoryImpl(Cryptor cryptor, CryptoWarningHandler cryptoWarningHandler) {
		this.cryptor = cryptor;
		this.cryptoWarningHandler = cryptoWarningHandler;
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		final Path path = ResourcePathUtils.getPhysicalPath(locator);
		final String rangeHeader = request.getHeader(HttpHeader.RANGE.asString());

		if (Files.isRegularFile(path) && DavMethods.METHOD_GET.equals(request.getMethod()) && rangeHeader != null) {
			response.setStatus(HttpStatus.SC_PARTIAL_CONTENT);
			return createFilePart(locator, request.getDavSession(), request);
		} else if (Files.isRegularFile(path) || DavMethods.METHOD_PUT.equals(request.getMethod())) {
			return createFile(locator, request.getDavSession());
		} else if (Files.isDirectory(path) || DavMethods.METHOD_MKCOL.equals(request.getMethod())) {
			return createDirectory(locator, request.getDavSession());
		} else {
			return createNonExisting(locator, request.getDavSession());
		}
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		final Path path = ResourcePathUtils.getPhysicalPath(locator);

		if (path != null && Files.isRegularFile(path)) {
			return createFile(locator, session);
		} else if (path != null && Files.isDirectory(path)) {
			return createDirectory(locator, session);
		} else {
			return createNonExisting(locator, session);
		}
	}

	private EncryptedFile createFilePart(DavResourceLocator locator, DavSession session, DavServletRequest request) {
		return new EncryptedFilePart(this, locator, session, request, lockManager, cryptor, cryptoWarningHandler);
	}

	private EncryptedFile createFile(DavResourceLocator locator, DavSession session) {
		return new EncryptedFile(this, locator, session, lockManager, cryptor, cryptoWarningHandler);
	}

	private EncryptedDir createDirectory(DavResourceLocator locator, DavSession session) {
		return new EncryptedDir(this, locator, session, lockManager, cryptor);
	}

	private NonExistingNode createNonExisting(DavResourceLocator locator, DavSession session) {
		return new NonExistingNode(this, locator, session, lockManager, cryptor);
	}

}

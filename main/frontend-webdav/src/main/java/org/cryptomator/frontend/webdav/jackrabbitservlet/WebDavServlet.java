/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.jackrabbit.FileSystemResourceLocatorFactory;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDavServlet extends AbstractWebdavServlet {

	private static final long serialVersionUID = -6632687979352625020L;

	private static final Logger LOG = LoggerFactory.getLogger(WebDavServlet.class);

	private final DavSessionProvider davSessionProvider;
	private final DavLocatorFactory davLocatorFactory;
	private final DavResourceFactory davResourceFactory;

	public WebDavServlet(URI contextRootUri, Folder root) {
		davSessionProvider = new DavSessionProviderImpl();
		davLocatorFactory = new FileSystemResourceLocatorFactory(contextRootUri, root);
		davResourceFactory = new FilesystemResourceFactory();
	}

	@Override
	protected boolean isPreconditionValid(WebdavRequest request, DavResource resource) {
		return !resource.exists() || request.matchesIfHeader(resource);
	}

	@Override
	public DavSessionProvider getDavSessionProvider() {
		return davSessionProvider;
	}

	@Override
	public void setDavSessionProvider(DavSessionProvider davSessionProvider) {
		throw new UnsupportedOperationException("Setting davSessionProvider not supported.");
	}

	@Override
	public DavLocatorFactory getLocatorFactory() {
		return davLocatorFactory;
	}

	@Override
	public void setLocatorFactory(DavLocatorFactory locatorFactory) {
		throw new UnsupportedOperationException("Setting locatorFactory not supported.");
	}

	@Override
	public DavResourceFactory getResourceFactory() {
		return davResourceFactory;
	}

	@Override
	public void setResourceFactory(DavResourceFactory resourceFactory) {
		throw new UnsupportedOperationException("Setting resourceFactory not supported.");
	}

	/* Unchecked DAV exception rewrapping */

	@Override
	protected boolean execute(WebdavRequest request, WebdavResponse response, int method, DavResource resource) throws ServletException, IOException, DavException {
		try {
			return super.execute(request, response, method, resource);
		} catch (UncheckedDavException e) {
			throw e.toDavException();
		}
	}

	/* GET stuff */

	@Override
	protected void doGet(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		try {
			super.doGet(request, response, resource);
		} catch (EofException e) {
			// Jetty EOF (other than IO EOF) is thrown when the connection is closed by the client.
			// If the client is no longer interested in further content, we don't care.
			if (LOG.isDebugEnabled()) {
				LOG.trace("Unexpected end of stream during GET (client hung up).");
			}
		}
	}

	/* LOCK stuff */

	@Override
	protected int validateDestination(DavResource destResource, WebdavRequest request, boolean checkHeader) throws DavException {
		if (isLocked(destResource) && !hasCorrectLockTokens(request.getDavSession(), destResource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The destination resource is locked");
		}
		return super.validateDestination(destResource, request, checkHeader);
	}

	@Override
	protected void doPut(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		if (isLocked(resource) && !hasCorrectLockTokens(request.getDavSession(), resource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The resource is locked");
		}
		super.doPut(request, response, resource);
	}

	@Override
	protected void doDelete(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		if (isLocked(resource) && !hasCorrectLockTokens(request.getDavSession(), resource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The resource is locked");
		}
		super.doDelete(request, response, resource);
	}

	@Override
	protected void doMove(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		if (isLocked(resource) && !hasCorrectLockTokens(request.getDavSession(), resource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The source resource is locked");
		}
		super.doMove(request, response, resource);
	}

	@Override
	protected void doPropPatch(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		if (isLocked(resource) && !hasCorrectLockTokens(request.getDavSession(), resource)) {
			throw new DavException(DavServletResponse.SC_LOCKED, "The resource is locked");
		}
		super.doPropPatch(request, response, resource);
	}

	private boolean hasCorrectLockTokens(DavSession session, DavResource resource) {
		boolean access = false;
		final String[] providedLockTokens = session.getLockTokens();
		for (ActiveLock lock : resource.getLocks()) {
			access |= ArrayUtils.contains(providedLockTokens, lock.getToken());
		}
		return access;
	}

	private boolean isLocked(DavResource resource) {
		return resource.hasLock(Type.WRITE, Scope.EXCLUSIVE) || resource.hasLock(Type.WRITE, Scope.SHARED);
	}

}

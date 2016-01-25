/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbitservlet;

import java.net.URI;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.jackrabbit.FileSystemResourceLocatorFactory;

class WebDavServlet extends AbstractWebdavServlet {

	private static final long serialVersionUID = -6632687979352625020L;

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

}

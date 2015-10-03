/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.crypto.exceptions.MacAuthenticationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDavServlet extends AbstractWebdavServlet {

	private static final long serialVersionUID = 7965170007048673022L;
	private static final Logger LOG = LoggerFactory.getLogger(WebDavServlet.class);
	public static final String CFG_FS_ROOT = "cfg.fs.root";
	private DavSessionProvider davSessionProvider;
	private DavLocatorFactory davLocatorFactory;
	private DavResourceFactory davResourceFactory;
	private final Cryptor cryptor;
	private final CryptoWarningHandler cryptoWarningHandler;

	public WebDavServlet(final Cryptor cryptor, final Collection<String> failingMacCollection, final Collection<String> whitelistedResourceCollection) {
		super();
		this.cryptor = cryptor;
		this.cryptoWarningHandler = new CryptoWarningHandler(failingMacCollection, whitelistedResourceCollection);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		final String fsRoot = config.getInitParameter(CFG_FS_ROOT);
		davSessionProvider = new DavSessionProviderImpl();
		davLocatorFactory = new CleartextLocatorFactory(config.getServletContext().getContextPath());
		davResourceFactory = new CryptoResourceFactory(cryptor, cryptoWarningHandler, fsRoot);
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
		this.davSessionProvider = davSessionProvider;
	}

	@Override
	public DavLocatorFactory getLocatorFactory() {
		return davLocatorFactory;
	}

	@Override
	public void setLocatorFactory(DavLocatorFactory locatorFactory) {
		this.davLocatorFactory = locatorFactory;
	}

	@Override
	public DavResourceFactory getResourceFactory() {
		return davResourceFactory;
	}

	@Override
	public void setResourceFactory(DavResourceFactory resourceFactory) {
		this.davResourceFactory = resourceFactory;
	}

	@Override
	protected void doPut(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		long t0 = System.nanoTime();
		super.doPut(request, response, resource);
		if (LOG.isDebugEnabled()) {
			long t1 = System.nanoTime();
			LOG.trace("PUT TIME: " + (t1 - t0) / 1000 / 1000.0 + " ms");
		}
	}

	@Override
	protected void doGet(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		long t0 = System.nanoTime();
		try {
			super.doGet(request, response, resource);
		} catch (MacAuthenticationFailedException e) {
			LOG.warn("File integrity violation for " + resource.getLocator().getResourcePath());
			cryptoWarningHandler.macAuthFailed(resource.getLocator().getResourcePath());
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
		if (LOG.isDebugEnabled()) {
			long t1 = System.nanoTime();
			LOG.trace("GET TIME: " + (t1 - t0) / 1000 / 1000.0 + " ms");
		}
	}

}

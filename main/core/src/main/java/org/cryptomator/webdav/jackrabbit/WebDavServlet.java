/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.util.Collection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;
import org.cryptomator.crypto.Cryptor;

public class WebDavServlet extends AbstractWebdavServlet {

	private static final long serialVersionUID = 7965170007048673022L;
	public static final String CFG_FS_ROOT = "cfg.fs.root";
	private DavSessionProvider davSessionProvider;
	private DavLocatorFactory davLocatorFactory;
	private DavResourceFactory davResourceFactory;
	private final Cryptor cryptor;
	private final CryptoWarningHandler cryptoWarningHandler;

	public WebDavServlet(final Cryptor cryptor, final Collection<String> failingMacCollection) {
		super();
		this.cryptor = cryptor;
		this.cryptoWarningHandler = new CryptoWarningHandler(failingMacCollection);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		davSessionProvider = new DavSessionProviderImpl();

		final String fsRoot = config.getInitParameter(CFG_FS_ROOT);
		this.davLocatorFactory = new DavLocatorFactoryImpl(fsRoot, cryptor);

		this.davResourceFactory = new DavResourceFactoryImpl(cryptor, cryptoWarningHandler);
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

}

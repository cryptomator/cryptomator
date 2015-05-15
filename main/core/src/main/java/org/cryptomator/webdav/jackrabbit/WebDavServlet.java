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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;
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
	private ExecutorService backgroundTaskExecutor;

	public WebDavServlet(final Cryptor cryptor, final Collection<String> failingMacCollection) {
		super();
		this.cryptor = cryptor;
		this.cryptoWarningHandler = new CryptoWarningHandler(failingMacCollection);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		final String fsRoot = config.getInitParameter(CFG_FS_ROOT);
		backgroundTaskExecutor = Executors.newCachedThreadPool();
		davSessionProvider = new DavSessionProviderImpl();
		davLocatorFactory = new CleartextLocatorFactory(config.getServletContext().getContextPath()); // CryptoLocatorFactory(fsRoot, cryptor);
		davResourceFactory = new CryptoResourceFactory(cryptor, cryptoWarningHandler, backgroundTaskExecutor, fsRoot);
	}

	@Override
	public void destroy() {
		backgroundTaskExecutor.shutdown();
		try {
			final boolean tasksFinished = backgroundTaskExecutor.awaitTermination(2, TimeUnit.SECONDS);
			if (!tasksFinished) {
				backgroundTaskExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			backgroundTaskExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		} finally {
			super.destroy();
		}
	}

	@Override
	protected void doMkCol(WebdavRequest request, WebdavResponse response, DavResource resource) throws IOException, DavException {
		if (resource instanceof EncryptedDirDuringCreation) {
			EncryptedDirDuringCreation dir = (EncryptedDirDuringCreation) resource;
			dir.doCreate();
			response.setStatus(DavServletResponse.SC_CREATED);
		} else {

		}
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

/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav;

import org.cryptomator.crypto.Cryptor;
import org.cryptomator.webdav.jackrabbit.WebDavServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebDAVServer {

	private static final Logger LOG = LoggerFactory.getLogger(WebDAVServer.class);
	private static final WebDAVServer INSTANCE = new WebDAVServer();
	private static final String LOCALHOST = "127.0.0.1";
	private final Server server = new Server();

	private WebDAVServer() {
		// make constructor private
	}

	public static WebDAVServer getInstance() {
		return INSTANCE;
	}

	/**
	 * @param workDir Path of encrypted folder.
	 * @param cryptor A fully initialized cryptor instance ready to en- or decrypt streams.
	 * @return port, on which the server did start
	 */
	public int start(final String workDir, final Cryptor cryptor) {
		final ServerConnector connector = new ServerConnector(server);
		connector.setHost(LOCALHOST);

		final String contextPath = "/";

		final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.addServlet(getMiltonServletHolder(workDir, contextPath, cryptor), "/*");
		context.setContextPath(contextPath);
		server.setHandler(context);

		try {
			server.setConnectors(new Connector[] {connector});
			server.start();
		} catch (Exception ex) {
			LOG.error("Server couldn't be started", ex);
		}

		return connector.getLocalPort();
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	public boolean stop() {
		try {
			server.stop();
		} catch (Exception ex) {
			LOG.error("Server couldn't be stopped", ex);
		}
		return server.isStopped();
	}

	private ServletHolder getMiltonServletHolder(final String workDir, final String contextPath, final Cryptor cryptor) {
		final ServletHolder result = new ServletHolder("Cryptomator-WebDAV-Servlet", new WebDavServlet(cryptor));
		result.setInitParameter(WebDavServlet.CFG_FS_ROOT, workDir);
		result.setInitParameter(WebDavServlet.CFG_HTTP_ROOT, contextPath);
		return result;
	}

}

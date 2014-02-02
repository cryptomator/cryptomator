/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.webdav;

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
	private final Server server = new Server();

	private WebDAVServer() {
		// make constructor private
	}

	public static WebDAVServer getInstance() {
		return INSTANCE;
	}

	public boolean start(final String workDir, final int port) {
		final ServerConnector connector = new ServerConnector(server);
		connector.setHost("127.0.0.1");
		connector.setPort(port);
		server.setConnectors(new Connector[] { connector });

		final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addServlet(getWebDAVServletHolder(workDir), "/*");
		server.setHandler(context);

		try {
			server.start();
		} catch (Exception ex) {
			LOG.error("Server couldn't be started", ex);
		}
		
		return server.isStarted();
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

	private ServletHolder getWebDAVServletHolder(final String rootpath) {
		final ServletHolder result = new ServletHolder("OCE-WebdavServlet", EnhancedWebDavServlet.class);
		result.setInitParameter("ResourceHandlerImplementation", FsWebdavResourceHandler.class.getName());
		result.setInitParameter("rootpath", rootpath);
		return result;
	}

}

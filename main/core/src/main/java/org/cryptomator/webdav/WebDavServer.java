/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.cryptomator.crypto.Cryptor;
import org.cryptomator.webdav.jackrabbit.WebDavServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebDavServer {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavServer.class);
	private static final String LOCALHOST = "::1";
	private static final int MAX_PENDING_REQUESTS = 200;
	private static final int MAX_THREADS = 200;
	private static final int MIN_THREADS = 4;
	private static final int THREAD_IDLE_SECONDS = 20;
	private final Server server;
	private int port;

	public WebDavServer() {
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_PENDING_REQUESTS);
		final ThreadPool tp = new QueuedThreadPool(MAX_THREADS, MIN_THREADS, THREAD_IDLE_SECONDS, queue);
		server = new Server(tp);
	}

	/**
	 * @param workDir Path of encrypted folder.
	 * @param cryptor A fully initialized cryptor instance ready to en- or decrypt streams.
	 * @return <code>true</code> upon success
	 */
	public synchronized boolean start(final String workDir, final Cryptor cryptor) {
		final ServerConnector connector = new ServerConnector(server);
		connector.setHost(LOCALHOST);

		final String contextPath = "/";
		final String servletPathSpec = "/*";

		final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.addServlet(getWebDavServletHolder(workDir, contextPath, cryptor), servletPathSpec);
		context.setContextPath(contextPath);
		server.setHandler(context);

		try {
			server.setConnectors(new Connector[] {connector});
			server.start();
			port = connector.getLocalPort();
			return true;
		} catch (Exception ex) {
			LOG.error("Server couldn't be started", ex);
			return false;
		}
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	public synchronized boolean stop() {
		try {
			server.stop();
			port = 0;
		} catch (Exception ex) {
			LOG.error("Server couldn't be stopped", ex);
		}
		return server.isStopped();
	}

	private ServletHolder getWebDavServletHolder(final String workDir, final String contextPath, final Cryptor cryptor) {
		final ServletHolder result = new ServletHolder("Cryptomator-WebDAV-Servlet", new WebDavServlet(cryptor));
		result.setInitParameter(WebDavServlet.CFG_FS_ROOT, workDir);
		result.setInitParameter(WebDavServlet.CFG_HTTP_ROOT, contextPath);
		return result;
	}

	public int getPort() {
		return port;
	}

}

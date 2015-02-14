/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.webdav.jackrabbit.WebDavServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;
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
	private final ServerConnector localConnector;
	private final ContextHandlerCollection servletCollection;

	public WebDavServer() {
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_PENDING_REQUESTS);
		final ThreadPool tp = new QueuedThreadPool(MAX_THREADS, MIN_THREADS, THREAD_IDLE_SECONDS, queue);
		server = new Server(tp);
		localConnector = new ServerConnector(server);
		localConnector.setHost(LOCALHOST);
		servletCollection = new ContextHandlerCollection();

		final ServletContextHandler servletContext = new ServletContextHandler(servletCollection, "/", ServletContextHandler.NO_SESSIONS);
		final ServletHolder servlet = new ServletHolder(WindowsSucksServlet.class);
		servletContext.addServlet(servlet, "/");

		server.setConnectors(new Connector[] {localConnector});
		server.setHandler(servletCollection);
	}

	public synchronized void start() {
		try {
			server.start();
			LOG.info("Cryptomator is running on port {}", getPort());
		} catch (Exception ex) {
			throw new RuntimeException("Server couldn't be started", ex);
		}
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	public synchronized void stop() {
		try {
			server.stop();
		} catch (Exception ex) {
			LOG.error("Server couldn't be stopped", ex);
		}
	}

	/**
	 * @param workDir Path of encrypted folder.
	 * @param cryptor A fully initialized cryptor instance ready to en- or decrypt streams.
	 * @param name The name of the folder. Must be non-empty and only contain any of
	 *            _ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789
	 * @return servlet
	 */
	public ServletLifeCycleAdapter createServlet(final Path workDir, final boolean checkFileIntegrity, final Cryptor cryptor, String name) {
		try {
			if (StringUtils.isEmpty(name)) {
				throw new IllegalArgumentException("name empty");
			}
			if (!StringUtils.containsOnly(name, "_ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")) {
				throw new IllegalArgumentException("name contains illegal characters: " + name);
			}
			final URI uri = new URI(null, null, localConnector.getHost(), localConnector.getLocalPort(), "/" + UUID.randomUUID().toString() + "/" + name, null, null);

			final ServletContextHandler servletContext = new ServletContextHandler(servletCollection, uri.getRawPath(), ServletContextHandler.SESSIONS);
			final ServletHolder servlet = getWebDavServletHolder(workDir.toString(), checkFileIntegrity, cryptor);
			servletContext.addServlet(servlet, "/*");

			servletCollection.mapContexts();

			LOG.debug("{} available on http:{}", workDir, uri.getRawSchemeSpecificPart());
			return new ServletLifeCycleAdapter(servletContext, uri);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid hard-coded URI components.", e);
		}
	}

	private ServletHolder getWebDavServletHolder(final String workDir, final boolean checkFileIntegrity, final Cryptor cryptor) {
		final ServletHolder result = new ServletHolder("Cryptomator-WebDAV-Servlet", new WebDavServlet(cryptor));
		result.setInitParameter(WebDavServlet.CFG_FS_ROOT, workDir);
		result.setInitParameter(WebDavServlet.CFG_CHECK_FILE_INTEGRITY, Boolean.toString(checkFileIntegrity));
		return result;
	}

	public int getPort() {
		return localConnector.getLocalPort();
	}

	/**
	 * Exposes implementation-specific methods to other modules.
	 */
	public class ServletLifeCycleAdapter {

		private final LifeCycle lifecycle;
		private final URI servletUri;

		private ServletLifeCycleAdapter(LifeCycle lifecycle, URI servletUri) {
			this.lifecycle = lifecycle;
			this.servletUri = servletUri;
		}

		public boolean isRunning() {
			return lifecycle.isRunning();
		}

		public boolean start() {
			try {
				lifecycle.start();
				return true;
			} catch (Exception e) {
				LOG.error("Failed to start", e);
				return false;
			}
		}

		public boolean stop() {
			try {
				lifecycle.stop();
				return true;
			} catch (Exception e) {
				LOG.error("Failed to stop", e);
				return false;
			}
		}

		public URI getServletUri() {
			return servletUri;
		}

	}

}

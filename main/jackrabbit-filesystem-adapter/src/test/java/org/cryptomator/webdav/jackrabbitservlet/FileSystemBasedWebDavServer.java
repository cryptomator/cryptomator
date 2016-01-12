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
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.DispatcherType;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.webdav.filters.AcceptRangeFilter;
import org.cryptomator.webdav.filters.LoggingHttpFilter;
import org.cryptomator.webdav.filters.PutIdleTimeoutFilter;
import org.cryptomator.webdav.filters.UriNormalizationFilter;
import org.cryptomator.webdav.filters.UriNormalizationFilter.ResourceTypeChecker;
import org.cryptomator.webdav.filters.UriNormalizationFilter.ResourceTypeChecker.ResourceType;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

class FileSystemBasedWebDavServer {

	private final Server server;
	private final ServerConnector localConnector;
	private final ContextHandlerCollection servletCollection;

	public FileSystemBasedWebDavServer(FileSystem fileSystem) {
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(10);
		final ThreadPool tp = new QueuedThreadPool(4, 1, 1, queue);
		server = new Server(tp);
		localConnector = new ServerConnector(server);
		localConnector.setHost("localhost");
		localConnector.setPort(8080);
		servletCollection = new ContextHandlerCollection();

		final ResourceTypeChecker resourceTypeChecker = (path) -> {
			if (fileSystem.resolveFolder(path).exists()) {
				return ResourceType.FOLDER;
			} else if (fileSystem.resolveFile(path).exists()) {
				return ResourceType.FILE;
			} else {
				return ResourceType.NONEXISTING;
			}
		};

		URI servletContextRootUri;
		try {
			servletContextRootUri = new URI("http", null, "localhost", 8080, "/", null, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
		final ServletContextHandler servletContext = new ServletContextHandler(servletCollection, "/", ServletContextHandler.SESSIONS);
		final ServletHolder servletHolder = new ServletHolder("FileSystem-WebDAV-Servlet", new WebDavServlet(servletContextRootUri, fileSystem));
		servletContext.addServlet(servletHolder, "/*");
		servletContext.addFilter(AcceptRangeFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(new FilterHolder(new UriNormalizationFilter(resourceTypeChecker)), "/*", EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(PutIdleTimeoutFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(LoggingHttpFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		servletCollection.mapContexts();

		server.setConnectors(new Connector[] {localConnector});
		server.setHandler(servletCollection);
	}

	public void start() throws Exception {
		server.start();
	}

	public void stop() throws Exception {
		server.stop();
	}

}

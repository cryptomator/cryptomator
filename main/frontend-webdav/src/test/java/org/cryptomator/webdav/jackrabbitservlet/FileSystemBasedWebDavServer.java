/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbitservlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.cryptomator.filesystem.FileSystem;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
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

		final URI servletContextRoot1;
		final URI servletContextRoot2;
		try {
			servletContextRoot1 = new URI("http", null, "localhost", 8080, "/foo/", null, null);
			servletContextRoot2 = new URI("http", null, "localhost", 8080, "/bar/", null, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}

		final WebDavServletContextFactory servletContextFactory = new WebDavServletContextFactory();
		servletCollection.addHandler(servletContextFactory.create(servletContextRoot1, fileSystem));
		servletCollection.addHandler(servletContextFactory.create(servletContextRoot2, fileSystem));
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

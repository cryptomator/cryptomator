/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.DispatcherType;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.inmem.InMemoryFileSystem;
import org.cryptomator.webdav.filters.AcceptRangeFilter;
import org.cryptomator.webdav.filters.UriNormalizationFilter;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

public class InMemoryWebDavServer {

	private final Server server;
	private final ServerConnector localConnector;
	private final ContextHandlerCollection servletCollection;
	private final FileSystem inMemoryFileSystem = new InMemoryFileSystem();

	private InMemoryWebDavServer() {
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(10);
		final ThreadPool tp = new QueuedThreadPool(4, 1, 1, queue);
		server = new Server(tp);
		localConnector = new ServerConnector(server);
		localConnector.setHost("localhost");
		localConnector.setPort(8080);
		servletCollection = new ContextHandlerCollection();

		URI servletContextRootUri;
		try {
			servletContextRootUri = new URI("http", null, "localhost", 8080, "/", null, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
		final ServletContextHandler servletContext = new ServletContextHandler(servletCollection, "/", ServletContextHandler.SESSIONS);
		final ServletHolder servletHolder = new ServletHolder("InMemory-WebDAV-Servlet", new WebDavServlet(servletContextRootUri, inMemoryFileSystem));
		servletContext.addServlet(servletHolder, "/*");
		servletContext.addFilter(AcceptRangeFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(UriNormalizationFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
		servletCollection.mapContexts();

		server.setConnectors(new Connector[] {localConnector});
		server.setHandler(servletCollection);
	}

	private void start() throws Exception {
		server.start();
	}

	private void stop() throws Exception {
		server.stop();
	}

	public static void main(String[] args) throws Exception {
		final InMemoryWebDavServer server = new InMemoryWebDavServer();

		server.inMemoryFileSystem.folder("mamals").folder("cats").create(FolderCreateMode.INCLUDING_PARENTS);
		server.inMemoryFileSystem.folder("mamals").folder("dogs").create(FolderCreateMode.INCLUDING_PARENTS);
		try (WritableFile writable = server.inMemoryFileSystem.folder("mamals").folder("cats").file("Garfield.txt").openWritable()) {
			writable.write(ByteBuffer.wrap("meow".getBytes()));
		}

		server.start();
		System.out.println("Server started. Press any key to stop it...");
		System.in.read();
		server.stop();
	}

}

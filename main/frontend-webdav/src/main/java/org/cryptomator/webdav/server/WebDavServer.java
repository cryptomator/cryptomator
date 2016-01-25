package org.cryptomator.webdav.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.filesystem.Folder;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class WebDavServer {

	private static final Logger LOG = LoggerFactory.getLogger(WebDavServer.class);
	private static final String LOCALHOST = SystemUtils.IS_OS_WINDOWS ? "::1" : "localhost";
	private static final int MAX_PENDING_REQUESTS = 200;
	private static final int MAX_THREADS = 200;
	private static final int MIN_THREADS = 4;
	private static final int THREAD_IDLE_SECONDS = 20;

	private final Server server;
	private final ServerConnector localConnector;
	private final ContextHandlerCollection servletCollection;
	private final WebDavServletContextFactory servletContextFactory;

	@Inject
	public WebDavServer(WebDavServletContextFactory servletContextFactory) {
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_PENDING_REQUESTS);
		final ThreadPool tp = new QueuedThreadPool(MAX_THREADS, MIN_THREADS, THREAD_IDLE_SECONDS, queue);
		this.server = new Server(tp);
		this.localConnector = new ServerConnector(server);
		this.servletCollection = new ContextHandlerCollection();
		this.servletContextFactory = servletContextFactory;

		localConnector.setHost(LOCALHOST);
		server.setConnectors(new Connector[] {localConnector});
		server.setHandler(servletCollection);
	}

	public void setPort(int port) {
		if (server.isStopped()) {
			localConnector.setPort(port);
		} else {
			throw new IllegalStateException("Cannot change port of running server.");
		}
	}

	public int getPort() {
		return localConnector.getLocalPort();
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

	public ServletContextHandler addServlet(Folder root, String contextPath) {
		final URI uri;
		try {
			uri = new URI("http", null, LOCALHOST, getPort(), contextPath, null, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
		ServletContextHandler handler = servletContextFactory.create(uri, root);
		servletCollection.addHandler(handler);
		servletCollection.mapContexts();
		return handler;
	}

}

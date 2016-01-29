package org.cryptomator.frontend.webdav;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.frontend.Frontend;
import org.cryptomator.frontend.FrontendCreationFailedException;
import org.cryptomator.frontend.FrontendFactory;
import org.cryptomator.frontend.webdav.mount.WebDavMounterProvider;
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
public class WebDavServer implements FrontendFactory {

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
	private final WebDavMounterProvider webdavMounterProvider;

	@Inject
	WebDavServer(WebDavServletContextFactory servletContextFactory, WebDavMounterProvider webdavMounterProvider) {
		final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(MAX_PENDING_REQUESTS);
		final ThreadPool tp = new QueuedThreadPool(MAX_THREADS, MIN_THREADS, THREAD_IDLE_SECONDS, queue);
		this.server = new Server(tp);
		this.localConnector = new ServerConnector(server);
		this.servletCollection = new ContextHandlerCollection();
		this.servletContextFactory = servletContextFactory;
		this.webdavMounterProvider = webdavMounterProvider;

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

	// visible for testing
	ServletContextHandler addServlet(Folder root, URI contextRoot) {
		ServletContextHandler handler = servletContextFactory.create(contextRoot, root);
		servletCollection.addHandler(handler);
		servletCollection.mapContexts();
		return handler;
	}

	@Override
	public Frontend create(Folder root, String contextPath) throws FrontendCreationFailedException {
		if (!contextPath.startsWith("/")) {
			throw new IllegalArgumentException("contextPath must begin with '/'");
		}
		final URI uri;
		try {
			uri = new URI("http", null, LOCALHOST, getPort(), contextPath, null, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
		final ServletContextHandler handler = addServlet(root, uri);
		LOG.info("Servlet available under " + uri);
		return new WebDavFrontend(webdavMounterProvider, handler, uri);
	}

}

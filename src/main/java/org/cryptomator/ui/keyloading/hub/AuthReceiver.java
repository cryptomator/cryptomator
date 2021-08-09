package org.cryptomator.ui.keyloading.hub;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import javax.servlet.DispatcherType;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A basic implementation for RFC 8252, Section 7.3:
 * <p>
 * We're spawning a local http server on a system-assigned high port and
 * use <code>http://127.0.0.1:{PORT}/success</code> as a redirect URI.
 * <p>
 * Furthermore, we can deliver a html response to inform the user that the
 * auth workflow finished and she can close the browser tab.
 */
class AuthReceiver implements AutoCloseable {

	private static final String REDIRECT_SCHEME = "http";
	private static final String LOOPBACK_ADDR = "127.0.0.1";
	private static final String JSON_200 = """
			{"status": "success"}
			""";
	private static final String JSON_400 = """
			{"status": "missing param"}
			""";

	private final Server server;
	private final ServerConnector connector;
	private final CallbackServlet servlet;

	private AuthReceiver(Server server, ServerConnector connector, CallbackServlet servlet) {
		assert server.isRunning();
		this.server = server;
		this.connector = connector;
		this.servlet = servlet;
	}

	public URI getRedirectURL() {
		try {
			return new URI(REDIRECT_SCHEME, null, LOOPBACK_ADDR, connector.getLocalPort(), null, null, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI constructed from well-formed components.", e);
		}
	}

	public static AuthReceiver start() throws Exception {
		var server = new Server();
		var context = new ServletContextHandler();

		var corsFilter = new FilterHolder(new CrossOriginFilter());
		corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*"); // TODO restrict to hub host
		context.addFilter(corsFilter, "/*", EnumSet.of(DispatcherType.REQUEST));

		var servlet = new CallbackServlet();
		context.addServlet(new ServletHolder(servlet), "/*");

		var connector = new ServerConnector(server);
		connector.setPort(0);
		connector.setHost(LOOPBACK_ADDR);
		server.setConnectors(new Connector[]{connector});
		server.setHandler(context);
		server.start();
		return new AuthReceiver(server, connector, servlet);
	}

	public AuthParams receive() throws InterruptedException {
		return servlet.receivedKeys.take();
	}

	@Override
	public void close() throws Exception {
		server.stop();
	}

	private static class CallbackServlet extends HttpServlet {

		private final BlockingQueue<AuthParams> receivedKeys = new LinkedBlockingQueue<>();

		// TODO change to POST?
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			var m = req.getParameter("m"); // encrypted masterkey
			var epk = req.getParameter("epk"); // ephemeral public key
			byte[] response;
			if (m != null && epk != null) {
				res.setStatus(HttpServletResponse.SC_OK);
				response = JSON_200.getBytes(StandardCharsets.UTF_8);
			} else {
				res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response = JSON_400.getBytes(StandardCharsets.UTF_8);
			}
			res.setContentType("application/json;charset=utf-8");
			res.setContentLength(response.length);
			res.getOutputStream().write(response);
			res.getOutputStream().flush();

			// the following line might trigger a server shutdown,
			// so let's make sure the response is flushed first
			if (m != null && epk != null) {
				receivedKeys.add(new AuthParams(m, epk));
			}
		}
	}
}

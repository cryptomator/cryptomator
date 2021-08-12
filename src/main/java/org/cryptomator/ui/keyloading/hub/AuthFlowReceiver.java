package org.cryptomator.ui.keyloading.hub;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
class AuthFlowReceiver implements AutoCloseable {

	private static final String LOOPBACK_ADDR = "127.0.0.1";
	private static final String CALLBACK_PATH = "/callback";
	private static final String HTML_SUCCESS = """
			<html>
			<head>
				<title>OAuth 2.0 Authentication Token Received</title>
			</head>
			<body>
				<p>Received verification code. You may now close this window.</p>
			</body>
			</html>
			""";

	private final Server server;
	private final ServerConnector connector;
	private final CallbackServlet servlet;

	private AuthFlowReceiver(Server server, ServerConnector connector, CallbackServlet servlet) {
		this.server = server;
		this.connector = connector;
		this.servlet = servlet;
	}

	public static AuthFlowReceiver start() throws Exception {
		var server = new Server();
		var context = new ServletContextHandler();

//		var corsFilter = new FilterHolder(new CrossOriginFilter());
//		corsFilter.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*"); // TODO restrict to hub host
//		context.addFilter(corsFilter, "/*", EnumSet.of(DispatcherType.REQUEST));

		var servlet = new CallbackServlet();
		context.addServlet(new ServletHolder(servlet), CALLBACK_PATH);

		var connector = new ServerConnector(server);
		connector.setPort(0);
		connector.setHost(LOOPBACK_ADDR);
		server.setConnectors(new Connector[]{connector});
		server.setHandler(context);
		server.start();
		return new AuthFlowReceiver(server, connector, servlet);
	}

	public String getRedirectUri() {
		return "http://" + LOOPBACK_ADDR + ":" + connector.getLocalPort() + CALLBACK_PATH;
	}

	public Callback receive() throws InterruptedException {
		return servlet.callback.take();
	}

	@Override
	public void close() throws Exception {
		server.stop();
	}

	public static record Callback(String error, String code, String state){}

	private static class CallbackServlet extends HttpServlet {

		private final BlockingQueue<Callback> callback = new LinkedBlockingQueue<>();

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			var error = req.getParameter("error");
			var code = req.getParameter("code");
			var state = req.getParameter("state");

			// TODO 302 use redirect to configurable site
			res.setContentType("text/html;charset=utf-8");
			res.getWriter().write(HTML_SUCCESS);
			res.getWriter().flush();

			callback.add(new Callback(error, code, state));
		}

	}

}

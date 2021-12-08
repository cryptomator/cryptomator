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
 * use <code>http://127.0.0.1:{PORT}/callback</code> as a redirect URI.
 * <p>
 * Furthermore, we can deliver a html response to inform the user that the
 * auth workflow finished and she can close the browser tab.
 */
class AuthFlowReceiver implements AutoCloseable {

	private static final String LOOPBACK_ADDR = "127.0.0.1";
	private static final String CALLBACK_PATH = "/callback";

	private final Server server;
	private final ServerConnector connector;
	private final CallbackServlet servlet;

	private AuthFlowReceiver(Server server, ServerConnector connector, CallbackServlet servlet) {
		this.server = server;
		this.connector = connector;
		this.servlet = servlet;
	}

	public static AuthFlowReceiver start(HubConfig hubConfig, AuthFlowContext authFlowContext) throws Exception {
		var server = new Server();
		var context = new ServletContextHandler();

		var servlet = new CallbackServlet(hubConfig, authFlowContext);
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

	public static record Callback(String error, String code, String state) {

	}

	private static class CallbackServlet extends HttpServlet {

		private final BlockingQueue<Callback> callback = new LinkedBlockingQueue<>();
		private final HubConfig hubConfig;
		private final AuthFlowContext authFlowContext;

		public CallbackServlet(HubConfig hubConfig, AuthFlowContext authFlowContext) {
			this.hubConfig = hubConfig;
			this.authFlowContext = authFlowContext;
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
			var error = req.getParameter("error");
			var code = req.getParameter("code");
			var state = req.getParameter("state");

			res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			if (error == null && code != null) {
				res.setHeader("Location", hubConfig.authSuccessUrl + "&device=" + authFlowContext.deviceId());
			} else {
				res.setHeader("Location", hubConfig.authErrorUrl + "&device=" + authFlowContext.deviceId());
			}

			callback.add(new Callback(error, code, state));
		}
	}

}

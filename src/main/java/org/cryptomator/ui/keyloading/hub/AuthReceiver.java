package org.cryptomator.ui.keyloading.hub;

import com.google.common.io.BaseEncoding;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TransferQueue;
import java.util.function.Consumer;

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
					{"status": "missing param key"}
					""";

	private final Server server;
	private final ServerConnector connector;
	private final Handler handler;

	private AuthReceiver(Server server, ServerConnector connector, Handler handler) {
		assert server.isRunning();
		this.server = server;
		this.connector = connector;
		this.handler = handler;
	}

	public URI getRedirectURL() {
		try {
			return new URI(REDIRECT_SCHEME, null, LOOPBACK_ADDR, connector.getLocalPort(), null, null, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI constructed from well-formed components.", e);
		}
	}

	public static AuthReceiver start() throws Exception {
		Server server = new Server();
		var handler = new Handler();
		var connector = new ServerConnector(server);
		connector.setPort(0);
		connector.setHost(LOOPBACK_ADDR);
		server.setConnectors(new Connector[]{connector});
		server.setHandler(handler);
		server.start();
		return new AuthReceiver(server, connector, handler);
	}

	public String receive() throws InterruptedException {
		return handler.receivedKeys.take();
	}

	@Override
	public void close() throws Exception {
		server.stop();
	}

	private static class Handler extends AbstractHandler {

		private final BlockingQueue<String> receivedKeys = new LinkedBlockingQueue<>();

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest req, HttpServletResponse res) throws IOException {
			baseRequest.setHandled(true);
			var key = req.getParameter("key");
			byte[] response;
			if (key != null) {
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
			if (key != null) {
				receivedKeys.add(key);
			}
		}
	}
}

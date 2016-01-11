package org.cryptomator.webdav.filters;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 * Wrapps the {@link ServletInputStream} into a timeout-aware stream, that returns EOF after a certain timeout.
 * Wrapping is done only for chunked PUT requests, as some WebDAV clients are too stupid to send a EOF-chunk (0-byte-chunk).
 */
public class PutIdleTimeoutFilter implements HttpFilter {

	private static final long TIMEOUT = 100;
	private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
	private static final String METHOD_PUT = "PUT";
	private static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
	private static final String HEADER_TRANSFER_ENCODING_CHUNKED = "chunked";

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}

	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (METHOD_PUT.equalsIgnoreCase(request.getMethod()) && HEADER_TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(request.getHeader(HEADER_TRANSFER_ENCODING))) {
			chain.doFilter(new PutRequestWithIdleTimeout(request), response);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		executor.shutdownNow();
	}

	private class PutRequestWithIdleTimeout extends HttpServletRequestWrapper {

		public PutRequestWithIdleTimeout(HttpServletRequest request) {
			super(request);
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return new IdleTimeoutServletInputStream(super.getInputStream());
		}

	}

	private class IdleTimeoutServletInputStream extends ServletInputStream {

		private final ServletInputStream delegate;
		private boolean timedOut = false;

		public IdleTimeoutServletInputStream(ServletInputStream delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean isFinished() {
			return timedOut || delegate.isFinished();
		}

		@Override
		public boolean isReady() {
			return !timedOut && delegate.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			delegate.setReadListener(readListener);
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			try {
				Future<Integer> readTask = executor.submit(() -> {
					return delegate.read(b, off, len);
				});
				return readTask.get(TIMEOUT, TIMEOUT_UNIT);
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			} catch (ExecutionException e) {
				throw new IOException("Exception during read", e);
			} catch (TimeoutException e) {
				timedOut = true;
				return -1;
			}
		}

		@Override
		public int read() throws IOException {
			try {
				Future<Integer> readTask = executor.submit(() -> {
					return delegate.read();
				});
				return readTask.get(TIMEOUT, TIMEOUT_UNIT);
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			} catch (ExecutionException e) {
				throw new IOException("Exception during read", e);
			} catch (TimeoutException e) {
				// throw new InterruptedByTimeoutException();
				timedOut = true;
				return -1;
			}
		}

	}

}

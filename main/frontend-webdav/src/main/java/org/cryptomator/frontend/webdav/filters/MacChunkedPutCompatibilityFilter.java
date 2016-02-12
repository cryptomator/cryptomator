/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.filters;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.input.BoundedInputStream;

/**
 * If a PUT request with chunked transfer encoding and a X-Expected-Entity-Length header field is sent,
 * the input stream will return EOF after the number of bytes stated in this header has been read.
 * 
 * This filter ensures compatibility of the Mac OS X WebDAV client, as Macs don't terminate chunked transfers normally (by sending a 0-byte-chunk).
 */
public class MacChunkedPutCompatibilityFilter implements HttpFilter {

	private static final String METHOD_PUT = "PUT";
	private static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
	private static final String HEADER_X_EXPECTED_ENTITIY_LENGTH = "X-Expected-Entity-Length";
	private static final String HEADER_TRANSFER_ENCODING_CHUNKED = "chunked";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}

	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		final String expectedEntitiyLengthHeader = request.getHeader(HEADER_X_EXPECTED_ENTITIY_LENGTH);
		if (METHOD_PUT.equalsIgnoreCase(request.getMethod()) //
				&& HEADER_TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(request.getHeader(HEADER_TRANSFER_ENCODING)) //
				&& expectedEntitiyLengthHeader != null) {
			long expectedEntitiyLength;
			try {
				expectedEntitiyLength = Long.valueOf(expectedEntitiyLengthHeader);
			} catch (NumberFormatException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Expected-Entity-Length");
				return;
			}
			chain.doFilter(new PutRequestWithBoundedInputStream(request, expectedEntitiyLength), response);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		// no-op
	}

	private static class PutRequestWithBoundedInputStream extends HttpServletRequestWrapper {

		private final long inputStreamLimit;

		public PutRequestWithBoundedInputStream(HttpServletRequest request, long inputStreamLimit) {
			super(request);
			this.inputStreamLimit = inputStreamLimit;
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return new BoundedServletInputStream(super.getInputStream(), inputStreamLimit);
		}

	}

	/**
	 * Like {@link BoundedInputStream}, but as a ServletInputStream.
	 */
	private static class BoundedServletInputStream extends ServletInputStream {

		private final BoundedInputStream boundedIn;
		private final ServletInputStream servletIn;
		private boolean reachedEof = false;
		private ReadListener readListener;

		public BoundedServletInputStream(ServletInputStream delegate, long limit) {
			this.boundedIn = new BoundedInputStream(delegate, limit);
			this.servletIn = delegate;
		}

		private void reachedEof() throws IOException {
			reachedEof = true;
			if (readListener != null) {
				readListener.onAllDataRead();
			}
		}

		/* BoundedInputStream */

		@Override
		public long skip(long n) throws IOException {
			return boundedIn.skip(n);
		}

		@Override
		public int available() throws IOException {
			return boundedIn.available();
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int read = boundedIn.read(b, off, len);
			if (read == -1) {
				reachedEof();
			}
			return read;
		}

		@Override
		public int read() throws IOException {
			int aByte = boundedIn.read();
			if (aByte == -1) {
				reachedEof();
			}
			return aByte;
		}

		/* ServletInputStream */

		@Override
		public boolean isFinished() {
			return reachedEof || servletIn.isFinished();
		}

		@Override
		public boolean isReady() {
			return !reachedEof && servletIn.isReady();
		}

		@Override
		public void setReadListener(ReadListener readListener) {
			servletIn.setReadListener(readListener);
			this.readListener = readListener;
		}

	}

}

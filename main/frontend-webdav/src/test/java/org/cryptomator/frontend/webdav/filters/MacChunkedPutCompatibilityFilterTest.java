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
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cryptomator.frontend.webdav.filters.MacChunkedPutCompatibilityFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class MacChunkedPutCompatibilityFilterTest {

	private MacChunkedPutCompatibilityFilter filter;
	private FilterChain chain;
	private HttpServletRequest request;
	private HttpServletResponse response;

	@Before
	public void setup() {
		filter = new MacChunkedPutCompatibilityFilter();
		chain = Mockito.mock(FilterChain.class);
		request = Mockito.mock(HttpServletRequest.class);
		response = Mockito.mock(HttpServletResponse.class);
	}

	@Test
	public void testUnfilteredGetRequest() throws IOException, ServletException {
		Mockito.when(request.getMethod()).thenReturn("GET");
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertSame(request, wrappedReq.getValue());
	}

	@Test
	public void testUnfilteredPutRequest1() throws IOException, ServletException {
		Mockito.when(request.getMethod()).thenReturn("PUT");
		Mockito.when(request.getHeader("Transfer-Encoding")).thenReturn(null);
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertSame(request, wrappedReq.getValue());
	}

	@Test
	public void testUnfilteredPutRequest2() throws IOException, ServletException {
		Mockito.when(request.getMethod()).thenReturn("PUT");
		Mockito.when(request.getHeader("Transfer-Encoding")).thenReturn("chunked");
		Mockito.when(request.getHeader("X-Expected-Entity-Length")).thenReturn(null);
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertSame(request, wrappedReq.getValue());
	}

	@Test
	public void testMalformedXExpectedEntityLengthHeader() throws IOException, ServletException {
		Mockito.when(request.getMethod()).thenReturn("PUT");
		Mockito.when(request.getHeader("Transfer-Encoding")).thenReturn("chunked");
		Mockito.when(request.getHeader("X-Expected-Entity-Length")).thenReturn("NaN");
		filter.doFilter(request, response, chain);

		Mockito.verify(response).sendError(Mockito.eq(HttpServletResponse.SC_BAD_REQUEST), Mockito.anyString());
		Mockito.verifyNoMoreInteractions(chain);
	}

	/* actual input stream testing */

	@Test
	public void testBoundedInputStream() throws IOException, ServletException {
		ServletInputStream in = Mockito.mock(ServletInputStream.class);

		Mockito.when(request.getMethod()).thenReturn("PUT");
		Mockito.when(request.getHeader("Transfer-Encoding")).thenReturn("chunked");
		Mockito.when(request.getHeader("X-Expected-Entity-Length")).thenReturn("5");
		Mockito.when(request.getInputStream()).thenReturn(in);
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		ServletInputStream wrappedIn = wrappedReq.getValue().getInputStream();

		Mockito.when(in.isFinished()).thenReturn(false);
		Assert.assertFalse(wrappedIn.isFinished());

		Mockito.when(in.isReady()).thenReturn(true);
		Assert.assertTrue(wrappedIn.isReady());

		Mockito.when(in.read()).thenReturn(0xFF);
		Assert.assertEquals(0xFF, wrappedIn.read());

		Mockito.when(in.available()).thenReturn(100);
		Assert.assertEquals(100, wrappedIn.available());

		Mockito.when(in.skip(2)).thenReturn(2l);
		Assert.assertEquals(2, wrappedIn.skip(2));

		Mockito.when(in.read(Mockito.any(), Mockito.eq(0), Mockito.eq(100))).thenReturn(100);
		Mockito.when(in.read(Mockito.any(), Mockito.eq(0), Mockito.eq(2))).thenReturn(2);
		Assert.assertEquals(2, wrappedIn.read(new byte[100], 0, 100));

		Mockito.when(in.read()).thenReturn(0xFF);
		Assert.assertEquals(-1, wrappedIn.read());

		Mockito.when(in.isFinished()).thenReturn(false);
		Assert.assertTrue(wrappedIn.isFinished());

		Mockito.when(in.isReady()).thenReturn(true);
		Assert.assertFalse(wrappedIn.isReady());
	}

}

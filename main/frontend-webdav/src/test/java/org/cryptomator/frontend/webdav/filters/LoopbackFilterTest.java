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
import java.net.InetAddress;
import java.util.Arrays;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assume;
import org.junit.Before;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(Theories.class)
public class LoopbackFilterTest {
	
	@DataPoints
	public static final Iterable<String> HOST_NAMES = Arrays.asList("127.0.0.1", "0::1", "1.2.3.4", "google.com");

	private LoopbackFilter filter;
	private FilterChain chain;
	private HttpServletRequest request;
	private HttpServletResponse response;

	@Before
	public void setup() {
		filter = new LoopbackFilter();
		chain = Mockito.mock(FilterChain.class);
		request = Mockito.mock(HttpServletRequest.class);
		response = Mockito.mock(HttpServletResponse.class);
	}
	
	@Theory
	public void testWithLoopbackAddress(String hostname) throws IOException, ServletException {
		Assume.assumeTrue(InetAddress.getByName(hostname).isLoopbackAddress());
		Mockito.when(request.getRemoteAddr()).thenReturn(hostname);
		
		filter.doFilter(request, response, chain);
		Mockito.verify(chain).doFilter(request, response);
	}
	
	@Theory
	public void testWithExternalAddress(String hostname) throws IOException, ServletException {
		Assume.assumeFalse(InetAddress.getByName(hostname).isLoopbackAddress());
		Mockito.when(request.getRemoteAddr()).thenReturn(hostname);
		
		filter.doFilter(request, response, chain);
		Mockito.verify(response).sendError(Mockito.eq(405), Mockito.anyString());
	}

}

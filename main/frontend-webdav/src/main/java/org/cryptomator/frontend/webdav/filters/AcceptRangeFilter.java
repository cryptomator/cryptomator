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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Adds an <code>Accept-Range: bytes</code> header to all <code>GET</code> requests.
 */
public class AcceptRangeFilter implements HttpFilter {

	private static final String METHOD_GET = "GET";
	private static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";
	private static final String HEADER_ACCEPT_RANGE_VALUE = "bytes";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}

	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (METHOD_GET.equalsIgnoreCase(request.getMethod())) {
			response.addHeader(HEADER_ACCEPT_RANGES, HEADER_ACCEPT_RANGE_VALUE);
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// no-op
	}

}

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

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Blocks all requests from external hosts.
 */
public class LoopbackFilter implements HttpFilter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}
	
	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) {
			chain.doFilter(request, response);
		} else {
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Can only access drive from localhost.");
		}
	}

	@Override
	public void destroy() {
		// no-op
	}

}

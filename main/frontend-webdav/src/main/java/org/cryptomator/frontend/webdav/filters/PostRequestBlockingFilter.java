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
 * Blocks all post requests.
 */
public class PostRequestBlockingFilter implements HttpFilter {

	private static final String POST_METHOD = "POST";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}
	
	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (isPost(request)) {
			response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		} else {
			chain.doFilter(request, new PostFromAllowHeaderRemovingHttpServletResponseWrapper(response));
		}
	}

	private boolean isPost(HttpServletRequest request) {
		return POST_METHOD.equalsIgnoreCase(request.getMethod());
	}

	@Override
	public void destroy() {
		// no-op
	}

}

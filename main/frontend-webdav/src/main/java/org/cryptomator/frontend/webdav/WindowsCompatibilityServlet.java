/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cryptomator.frontend.webdav.filters.LoopbackFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * The server needs to respond to requests to the root resource, because Windows is stupid. 
 */
public class WindowsCompatibilityServlet extends HttpServlet {
	
	private static final String ROOT_PATH = "/";
	
	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.addHeader("DAV", "1, 2");
		resp.addHeader("MS-Author-Via", "DAV");
		// resp.addHeader("Allow", "OPTIONS, GET, HEAD, POST, TRACE, PROPFIND, PROPPATCH, MKCOL, COPY, PUT, DELETE, MOVE, LOCK, UNLOCK");
		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}
	
	public static ServletContextHandler createServletContextHandler() {
		final ServletContextHandler servletContext = new ServletContextHandler(null, ROOT_PATH, ServletContextHandler.NO_SESSIONS);
		final ServletHolder servletHolder = new ServletHolder(ROOT_PATH, WindowsCompatibilityServlet.class);
		servletContext.addServlet(servletHolder, ROOT_PATH);
		servletContext.addFilter(LoopbackFilter.class, ROOT_PATH, EnumSet.of(DispatcherType.REQUEST));
		return servletContext;
	}

}

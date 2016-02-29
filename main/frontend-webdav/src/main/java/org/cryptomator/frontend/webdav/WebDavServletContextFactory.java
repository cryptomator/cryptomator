/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav;

import java.net.URI;
import java.util.EnumSet;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.frontend.webdav.filters.AcceptRangeFilter;
import org.cryptomator.frontend.webdav.filters.LoopbackFilter;
import org.cryptomator.frontend.webdav.filters.MacChunkedPutCompatibilityFilter;
import org.cryptomator.frontend.webdav.filters.MkcolComplianceFilter;
import org.cryptomator.frontend.webdav.filters.UriNormalizationFilter;
import org.cryptomator.frontend.webdav.filters.UriNormalizationFilter.ResourceTypeChecker;
import org.cryptomator.frontend.webdav.filters.UriNormalizationFilter.ResourceTypeChecker.ResourceType;
import org.cryptomator.frontend.webdav.jackrabbitservlet.WebDavServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@Singleton
class WebDavServletContextFactory {

	private static final String WILDCARD = "/*";

	@Inject
	public WebDavServletContextFactory() {
	}

	/**
	 * Creates a new Jetty ServletContextHandler, that can be be added to a servletCollection as follows:
	 * 
	 * <pre>
	 * ServletContextHandler context = factory.create(...);
	 * servletCollection.addHandler(context);
	 * servletCollection.mapContexts();
	 * </pre>
	 * 
	 * @param contextRoot The URI of the context root. Its path will be used as the servlet's context path.
	 * @param root The location within a filesystem that shall be served via WebDAV.
	 * @return A new Jetty servlet context handler.
	 */
	public ServletContextHandler create(URI contextRoot, Folder root) {
		final ResourceTypeChecker resourceTypeChecker = (path) -> {
			if (root.resolveFolder(path).exists()) {
				return ResourceType.FOLDER;
			} else if (root.resolveFile(path).exists()) {
				return ResourceType.FILE;
			} else {
				return ResourceType.UNKNOWN;
			}
		};
		final String contextPath = StringUtils.removeEnd(contextRoot.getPath(), "/");
		final ServletContextHandler servletContext = new ServletContextHandler(null, contextPath, ServletContextHandler.SESSIONS);
		final ServletHolder servletHolder = new ServletHolder(contextPath, new WebDavServlet(contextRoot, root));
		servletContext.addServlet(servletHolder, WILDCARD);
		servletContext.addFilter(LoopbackFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(MkcolComplianceFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(AcceptRangeFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		servletContext.addFilter(new FilterHolder(new UriNormalizationFilter(resourceTypeChecker)), WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		if (SystemUtils.IS_OS_MAC_OSX) {
			servletContext.addFilter(MacChunkedPutCompatibilityFilter.class, WILDCARD, EnumSet.of(DispatcherType.REQUEST));
		}
		return servletContext;
	}

}

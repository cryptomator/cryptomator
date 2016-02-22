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
import java.net.URI;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.frontend.webdav.filters.UriNormalizationFilter.ResourceTypeChecker.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Normalizes all URIs contained in requests depending on the resource type of existing resources.
 * URIs identifying directories will always end on "/", URIs identifying files will not.
 * 
 * If the resource type is unknown, because the resource doesn't exist yet, this filter will determine the resource type based on the HTTP method,
 * e.g. a MKCOL request will result in a directory..
 */
public class UriNormalizationFilter implements HttpFilter {

	private static final Logger LOG = LoggerFactory.getLogger(UriNormalizationFilter.class);
	private static final String[] FILE_METHODS = {"PUT"};
	private static final String[] DIRECTORY_METHODS = {"MKCOL"};

	@FunctionalInterface
	public interface ResourceTypeChecker {

		enum ResourceType {
			FILE, FOLDER, UNKNOWN;
		};

		/**
		 * Checks if the resource with the given resource name is a file, a folder or doesn't exist.
		 * 
		 * @param resourcePath Relative URI of the resource in question.
		 * @return Type of the resource or {@link ResourceType#UNKNOWN UNKNOWN} for non-existing resources. Never <code>null</code>.
		 */
		ResourceType typeOfResource(String resourcePath);

	}

	private final ResourceTypeChecker resourceTypeChecker;
	private String contextPath;

	public UriNormalizationFilter(ResourceTypeChecker resourceTypeChecker) {
		this.resourceTypeChecker = resourceTypeChecker;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		contextPath = filterConfig.getServletContext().getContextPath();
	}

	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		final ResourceType resourceType = resourceTypeChecker.typeOfResource(request.getPathInfo());
		final HttpServletRequest normalizedRequest;
		switch (resourceType) {
		case FILE:
			normalizedRequest = normalizedFileRequest(request);
			break;
		case FOLDER:
			normalizedRequest = normalizedFolderRequest(request);
			break;
		default:
			normalizedRequest = normalizedRequestForUnknownResource(request);
			break;
		}
		chain.doFilter(normalizedRequest, response);
	}

	@Override
	public void destroy() {
		// no-op
	}

	private HttpServletRequest normalizedFileRequest(HttpServletRequest originalRequest) {
		LOG.trace("Treating resource as file: {}", originalRequest.getRequestURI());
		return new FileUriRequest(originalRequest);
	}

	private HttpServletRequest normalizedFolderRequest(HttpServletRequest originalRequest) {
		LOG.trace("Treating resource as folder: {}", originalRequest.getRequestURI());
		return new FolderUriRequest(originalRequest);
	}

	private HttpServletRequest normalizedRequestForUnknownResource(HttpServletRequest originalRequest) {
		final String requestMethod = originalRequest.getMethod().toUpperCase();
		if (ArrayUtils.contains(FILE_METHODS, requestMethod)) {
			return normalizedFileRequest(originalRequest);
		} else if (ArrayUtils.contains(DIRECTORY_METHODS, requestMethod)) {
			return normalizedFolderRequest(originalRequest);
		} else {
			LOG.trace("Could not determine resource type of resource: {}", originalRequest.getRequestURI());
			return originalRequest;
		}
	}

	/**
	 * Adjusts headers containing URIs depending on the request URI.
	 */
	private class SuffixPreservingRequest extends HttpServletRequestWrapper {

		private static final String HEADER_DESTINATION = "Destination";
		private static final String METHOD_MOVE = "MOVE";
		private static final String METHOD_COPY = "COPY";

		public SuffixPreservingRequest(HttpServletRequest request) {
			super(request);
			request.getContextPath();
		}

		@Override
		public String getHeader(String name) {
			if ((METHOD_MOVE.equalsIgnoreCase(getMethod()) || METHOD_COPY.equalsIgnoreCase(getMethod())) && HEADER_DESTINATION.equalsIgnoreCase(name)) {
				return bestGuess(URI.create(super.getHeader(name)));
			} else {
				return super.getHeader(name);
			}
		}

		private String bestGuess(URI uri) {
			final String pathWithinContext = StringUtils.removeStart(uri.getPath(), contextPath);
			final ResourceType resourceType = resourceTypeChecker.typeOfResource(pathWithinContext);
			switch (resourceType) {
			case FILE:
				return asFileUri(uri.getRawPath());
			case FOLDER:
				return asFolderUri(uri.getRawPath());
			default:
				if (this.getRequestURI().endsWith("/")) {
					return asFolderUri(uri.getRawPath());
				} else {
					return asFileUri(uri.getRawPath());
				}
			}
		}

		protected String asFileUri(String uri) {
			return StringUtils.removeEnd(uri, "/");
		}

		protected String asFolderUri(String uri) {
			return StringUtils.appendIfMissing(uri, "/");
		}

	}

	/**
	 * HTTP request, whose URI never ends on "/".
	 */
	private class FileUriRequest extends SuffixPreservingRequest {

		public FileUriRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getRequestURI() {
			return asFileUri(super.getRequestURI());
		}

	}

	/**
	 * HTTP request, whose URI always ends on "/".
	 */
	private class FolderUriRequest extends SuffixPreservingRequest {

		public FolderUriRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getRequestURI() {
			return asFolderUri(super.getRequestURI());
		}

	}

}

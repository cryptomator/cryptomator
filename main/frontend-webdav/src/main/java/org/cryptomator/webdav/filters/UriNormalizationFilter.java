/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.filters;

import java.io.IOException;
import java.util.function.Function;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.cryptomator.webdav.filters.UriNormalizationFilter.ResourceTypeChecker.ResourceType;
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

	private final ResourceTypeChecker resourceTypeChecker;

	public UriNormalizationFilter(ResourceTypeChecker resourceTypeChecker) {
		this.resourceTypeChecker = resourceTypeChecker;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// no-op
	}

	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		ResourceType resourceType = resourceTypeChecker.typeOfResource(request.getPathInfo());
		HttpServletRequest normalizedRequest = resourceType.normalizedRequest(request);
		chain.doFilter(normalizedRequest, response);
	}

	@Override
	public void destroy() {
		// no-op
	}

	private static HttpServletRequest normalizedFileRequest(HttpServletRequest originalRequest) {
		LOG.debug("Treating resource as file: {}", originalRequest.getRequestURI());
		return new FileUriRequest(originalRequest);
	}

	private static HttpServletRequest normalizedFolderRequest(HttpServletRequest originalRequest) {
		LOG.debug("Treating resource as folder: {}", originalRequest.getRequestURI());
		return new FolderUriRequest(originalRequest);
	}

	private static HttpServletRequest normalizedRequestForUnknownResource(HttpServletRequest originalRequest) {
		final String requestMethod = originalRequest.getMethod().toUpperCase();
		if (ArrayUtils.contains(FILE_METHODS, requestMethod)) {
			return normalizedFileRequest(originalRequest);
		} else if (ArrayUtils.contains(DIRECTORY_METHODS, requestMethod)) {
			return normalizedFolderRequest(originalRequest);
		} else {
			LOG.debug("Could not determine resource type of resource: {}", originalRequest.getRequestURI());
			return originalRequest;
		}
	}

	@FunctionalInterface
	public interface ResourceTypeChecker {

		enum ResourceType {
			FILE(UriNormalizationFilter::normalizedFileRequest), //
			FOLDER(UriNormalizationFilter::normalizedFolderRequest), //
			UNKNOWN(UriNormalizationFilter::normalizedRequestForUnknownResource);

			private final Function<HttpServletRequest, HttpServletRequest> wrapper;

			private ResourceType(Function<HttpServletRequest, HttpServletRequest> wrapper) {
				this.wrapper = wrapper;
			}

			private HttpServletRequest normalizedRequest(HttpServletRequest request) {
				return wrapper.apply(request);
			}
		};

		/**
		 * Checks if the resource with the given resource name is a file, a folder or doesn't exist.
		 * 
		 * @param resourcePath Relative URI of the resource in question.
		 * @return Type of the resource or {@link ResourceType#UNKNOWN UNKNOWN} for non-existing resources. Never <code>null</code>.
		 */
		ResourceType typeOfResource(String resourcePath);

	}

	/**
	 * Adjusts headers containing URIs depending on the request URI.
	 */
	private static class SuffixPreservingRequest extends HttpServletRequestWrapper {

		private static final String HEADER_DESTINATION = "Destination";
		private static final String METHOD_MOVE = "MOVE";
		private static final String METHOD_COPY = "COPY";

		public SuffixPreservingRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getHeader(String name) {
			if ((METHOD_MOVE.equalsIgnoreCase(getMethod()) || METHOD_COPY.equalsIgnoreCase(getMethod())) && HEADER_DESTINATION.equalsIgnoreCase(name)) {
				return sameSuffixAsUri(super.getHeader(name));
			} else {
				return super.getHeader(name);
			}
		}

		private String sameSuffixAsUri(String str) {
			final String uri = this.getRequestURI();
			if (uri.endsWith("/")) {
				return StringUtils.appendIfMissing(str, "/");
			} else {
				return StringUtils.removeEnd(str, "/");
			}
		}

	}

	/**
	 * HTTP request, whose URI never ends on "/".
	 */
	private static class FileUriRequest extends SuffixPreservingRequest {

		public FileUriRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getRequestURI() {
			return StringUtils.removeEnd(super.getRequestURI(), "/");
		}

	}

	/**
	 * HTTP request, whose URI always ends on "/".
	 */
	private static class FolderUriRequest extends SuffixPreservingRequest {

		public FolderUriRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getRequestURI() {
			return StringUtils.appendIfMissing(super.getRequestURI(), "/");
		}

	}

}

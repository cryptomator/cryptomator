package org.cryptomator.webdav.filters;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
			FILE, FOLDER, NONEXISTING
		};

		ResourceType typeOfResource(String resourcePath);

	}

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
		switch (resourceTypeChecker.typeOfResource(request.getPathInfo())) {
		case FILE:
			chain.doFilter(new FileUriRequest(request), response);
			return;
		case FOLDER:
			chain.doFilter(new FolderUriRequest(request), response);
			return;
		case NONEXISTING:
		default:
			if (ArrayUtils.contains(FILE_METHODS, request.getMethod().toUpperCase())) {
				chain.doFilter(new FileUriRequest(request), response);
			} else if (ArrayUtils.contains(DIRECTORY_METHODS, request.getMethod().toUpperCase())) {
				chain.doFilter(new FolderUriRequest(request), response);
			} else {
				LOG.warn("Could not determine resource type for URI {}. Leaving request unmodified.", request.getRequestURI());
				chain.doFilter(request, response);
			}
		}
	}

	@Override
	public void destroy() {
		// no-op
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
			LOG.debug("Treating resource as file: {}", request.getRequestURI());
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
			LOG.debug("Treating resource as folder: {}", request.getRequestURI());
		}

		@Override
		public String getRequestURI() {
			return StringUtils.appendIfMissing(super.getRequestURI(), "/");
		}

	}

}

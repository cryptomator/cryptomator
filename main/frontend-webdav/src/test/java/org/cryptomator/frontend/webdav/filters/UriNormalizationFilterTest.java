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
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cryptomator.frontend.webdav.filters.UriNormalizationFilter;
import org.cryptomator.frontend.webdav.filters.UriNormalizationFilter.ResourceTypeChecker;
import org.cryptomator.frontend.webdav.filters.UriNormalizationFilter.ResourceTypeChecker.ResourceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class UriNormalizationFilterTest {

	private ResourceTypeChecker resourceTypeChecker;
	private UriNormalizationFilter filter;
	private FilterChain chain;
	private HttpServletRequest request;
	private HttpServletResponse response;

	@Before
	public void setup() {
		resourceTypeChecker = Mockito.mock(ResourceTypeChecker.class);
		filter = new UriNormalizationFilter(resourceTypeChecker);
		chain = Mockito.mock(FilterChain.class);
		request = Mockito.mock(HttpServletRequest.class);
		response = Mockito.mock(HttpServletResponse.class);

		Mockito.when(resourceTypeChecker.typeOfResource("/file")).thenReturn(ResourceType.FILE);
		Mockito.when(resourceTypeChecker.typeOfResource("/file/")).thenReturn(ResourceType.FILE);
		Mockito.when(resourceTypeChecker.typeOfResource("/folder")).thenReturn(ResourceType.FOLDER);
		Mockito.when(resourceTypeChecker.typeOfResource("/folder/")).thenReturn(ResourceType.FOLDER);
		Mockito.when(resourceTypeChecker.typeOfResource("/404")).thenReturn(ResourceType.UNKNOWN);
		Mockito.when(resourceTypeChecker.typeOfResource("/404/")).thenReturn(ResourceType.UNKNOWN);
	}

	/* FILE */

	@Test
	public void testFileRequest1() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/file");
		Mockito.when(request.getRequestURI()).thenReturn("/file");
		filter.doFilter(request, response, chain);
		Mockito.verify(resourceTypeChecker).typeOfResource("/file");

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/file", wrappedReq.getValue().getRequestURI());
	}

	@Test
	public void testFileRequest2() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/file/");
		Mockito.when(request.getRequestURI()).thenReturn("/file/");
		filter.doFilter(request, response, chain);
		Mockito.verify(resourceTypeChecker).typeOfResource("/file/");

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/file", wrappedReq.getValue().getRequestURI());
	}

	@Test
	public void testCopyFileRequest() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/file/");
		Mockito.when(request.getRequestURI()).thenReturn("/file/");
		Mockito.when(request.getMethod()).thenReturn("COPY");
		Mockito.when(request.getHeader("Destination")).thenReturn("/404/");
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/404", wrappedReq.getValue().getHeader("Destination"));
	}

	/* FOLDER */

	@Test
	public void testFolderRequest1() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/folder");
		Mockito.when(request.getRequestURI()).thenReturn("/folder");
		filter.doFilter(request, response, chain);
		Mockito.verify(resourceTypeChecker).typeOfResource("/folder");

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/folder/", wrappedReq.getValue().getRequestURI());
	}

	@Test
	public void testFolderRequest2() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/folder/");
		Mockito.when(request.getRequestURI()).thenReturn("/folder/");
		filter.doFilter(request, response, chain);
		Mockito.verify(resourceTypeChecker).typeOfResource("/folder/");

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/folder/", wrappedReq.getValue().getRequestURI());
	}

	@Test
	public void testMoveFolderRequest() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/folder");
		Mockito.when(request.getRequestURI()).thenReturn("/folder");
		Mockito.when(request.getMethod()).thenReturn("MOVE");
		Mockito.when(request.getHeader("Destination")).thenReturn("/404");
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/404/", wrappedReq.getValue().getHeader("Destination"));
	}

	/* MIXED */

	@Test
	public void testCopyFileToFolderRequest() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/file/");
		Mockito.when(request.getRequestURI()).thenReturn("/file/");
		Mockito.when(request.getMethod()).thenReturn("COPY");
		Mockito.when(request.getHeader("Destination")).thenReturn("/folder");
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/file", wrappedReq.getValue().getRequestURI());
		Assert.assertEquals("/folder/", wrappedReq.getValue().getHeader("Destination"));
	}

	@Test
	public void testMoveFolderToFileRequest() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/folder");
		Mockito.when(request.getRequestURI()).thenReturn("/folder");
		Mockito.when(request.getMethod()).thenReturn("COPY");
		Mockito.when(request.getHeader("Destination")).thenReturn("/file/");
		filter.doFilter(request, response, chain);

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/folder/", wrappedReq.getValue().getRequestURI());
		Assert.assertEquals("/file", wrappedReq.getValue().getHeader("Destination"));
	}

	/* UNKNOWN */

	@Test
	public void testUnknownPutRequest() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/404/");
		Mockito.when(request.getRequestURI()).thenReturn("/404/");
		Mockito.when(request.getMethod()).thenReturn("PUT");
		filter.doFilter(request, response, chain);
		Mockito.verify(resourceTypeChecker).typeOfResource("/404/");

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/404", wrappedReq.getValue().getRequestURI());
	}

	@Test
	public void testUnknownMkcolRequest() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/404");
		Mockito.when(request.getRequestURI()).thenReturn("/404");
		Mockito.when(request.getMethod()).thenReturn("MKCOL");
		filter.doFilter(request, response, chain);
		Mockito.verify(resourceTypeChecker).typeOfResource("/404");

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertEquals("/404/", wrappedReq.getValue().getRequestURI());
	}

	@Test
	public void testUnknownPropfindRequest() throws IOException, ServletException {
		Mockito.when(request.getPathInfo()).thenReturn("/404");
		Mockito.when(request.getRequestURI()).thenReturn("/404");
		Mockito.when(request.getMethod()).thenReturn("PROPFIND");
		filter.doFilter(request, response, chain);
		Mockito.verify(resourceTypeChecker).typeOfResource("/404");

		ArgumentCaptor<HttpServletRequest> wrappedReq = ArgumentCaptor.forClass(HttpServletRequest.class);
		Mockito.verify(chain).doFilter(wrappedReq.capture(), Mockito.any(ServletResponse.class));
		Assert.assertSame(request, wrappedReq.getValue());
	}

}

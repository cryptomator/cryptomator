/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.filters;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cryptomator.frontend.webdav.filters.HttpFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingHttpFilter implements HttpFilter {

	private static final Set<String> METHODS_TO_LOG_DETAILED = methodsToLog();

	private static final Set<String> methodsToLog() {
		String methodsToLog = System.getProperty("cryptomator.LoggingHttpFilter.methodsToLogDetailed");
		if (methodsToLog == null) {
			return Collections.emptySet();
		} else {
			return new HashSet<>(asList(methodsToLog.toUpperCase().split(",")));
		}
	}

	private final Logger LOG = LoggerFactory.getLogger(LoggingHttpFilter.class);

	@Override
	public void doFilterHttp(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (METHODS_TO_LOG_DETAILED.contains(request.getMethod().toUpperCase())) {
			logDetailed(request, response, chain);
		} else {
			logBasic(request, response, chain);
		}
	}

	private void logBasic(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		Optional<Throwable> thrown = Optional.empty();
		try {
			chain.doFilter(request, response);
		} catch (IOException | ServletException e) {
			thrown = Optional.of(e);
			throw e;
		} catch (RuntimeException | Error e) {
			thrown = Optional.of(e);
			throw e;
		} finally {
			if (thrown.isPresent()) {
				logError(request, thrown.get());
			} else {
				logSuccess(request, response);
			}
		}
	}

	private void logDetailed(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		RecordingHttpServletRequest recordingRequest = new RecordingHttpServletRequest(request);
		RecordingHttpServletResponse recordingResponse = new RecordingHttpServletResponse(response);
		Optional<Throwable> thrown = Optional.empty();
		try {
			chain.doFilter(recordingRequest, recordingResponse);
		} catch (IOException | ServletException e) {
			thrown = Optional.of(e);
			throw e;
		} catch (RuntimeException | Error e) {
			thrown = Optional.of(e);
			throw e;
		} finally {
			if (thrown.isPresent()) {
				logError(recordingRequest, thrown.get());
			} else {
				logSuccess(recordingRequest, recordingResponse);
			}
		}
	}

	private void logSuccess(HttpServletRequest request, HttpServletResponse response) {
		LOG.debug(format(
				"## Request ##\n" + //
						"%s %s %s\n" //
						+ "%s\n" //
						+ "## Response ##\n" //
						+ "%s %s\n" //
						+ "%s\n", //
				request.getMethod(), request.getRequestURI(), request.getProtocol(), //
				headers(request), //
				request.getProtocol(), response.getStatus(), //
				headers(response)));
	}

	private void logError(HttpServletRequest request, Throwable throwable) {
		LOG.error(
				format("## Request ##\n" + //
						"%s %s %s\n" //
						+ "%s\n", //
				request.getMethod(), request.getRequestURI(), request.getProtocol(), //
				headers(request)), //
				throwable);
	}

	private void logSuccess(RecordingHttpServletRequest request, RecordingHttpServletResponse response) {
		LOG.debug(format(
				"## Request ##\n" + //
						"%s %s %s\n" //
						+ "%s\n" //
						+ "%s\n\n" //
						+ "## Response ##\n" //
						+ "%s %s\n" //
						+ "%s\n" //
						+ "%s", //
				request.getMethod(), request.getRequestURI(), request.getProtocol(), //
				headers(request), //
				new String(request.getRecording()), //
				request.getProtocol(), response.getStatus(), //
				headers(response), //
				new String(response.getRecording())));
	}

	private void logError(RecordingHttpServletRequest request, Throwable throwable) {
		LOG.error(
				format("## Request ##\n" + //
						"%s %s %s\n" //
						+ "%s\n" //
						+ "%s\n\n", //
				request.getMethod(), request.getRequestURI(), request.getProtocol(), //
				headers(request), //
				new String(request.getRecording())), //
				throwable);
	}

	private String headers(HttpServletResponse response) {
		StringBuilder result = new StringBuilder();
		for (String headerName : response.getHeaderNames()) {
			for (String value : response.getHeaders(headerName)) {
				result.append(headerName).append(": ").append(value).append('\n');
			}
		}
		return result.toString();
	}

	private String headers(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			Enumeration<String> values = request.getHeaders(headerName);
			while (values.hasMoreElements()) {
				result.append(headerName).append(": ").append(values.nextElement()).append('\n');
			}
		}
		return result.toString();
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// empty
	}

	@Override
	public void destroy() {
		// empty
	}

}

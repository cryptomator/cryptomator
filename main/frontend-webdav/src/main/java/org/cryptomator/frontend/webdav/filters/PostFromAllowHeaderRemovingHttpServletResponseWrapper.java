package org.cryptomator.frontend.webdav.filters;

import static java.util.Arrays.stream;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.joining;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

class PostFromAllowHeaderRemovingHttpServletResponseWrapper extends HttpServletResponseWrapper {
	
	public PostFromAllowHeaderRemovingHttpServletResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	@Override
	public void addHeader(String name, String value) {
		if (isAllowHeader(name)) {
			super.setHeader(name, removePost(value));
		} else {
			super.addHeader(name, value);
		}
	}

	@Override
	public void setHeader(String name, String value) {
		if (isAllowHeader(name)) {
			super.setHeader(name, removePost(value));
		} else {
			super.setHeader(name, value);
		}
	}

	private String removePost(String value) {
		return stream(value.split("\\s*,\\s*"))
				.filter(isEqual("POST").negate())
				.collect(joining(", "));
	}

	private boolean isAllowHeader(String name) {
		return "allow".equalsIgnoreCase(name);
	}
}
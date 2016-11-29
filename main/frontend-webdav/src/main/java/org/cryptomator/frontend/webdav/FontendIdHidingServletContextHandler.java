package org.cryptomator.frontend.webdav;

import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.servlet.ServletContextHandler;

class FontendIdHidingServletContextHandler extends ServletContextHandler {

	public FontendIdHidingServletContextHandler(HandlerContainer parent, String contextPath, int options) {
		super(parent, contextPath, options);
	}

	@Override
	public String toString() {
		return ContextPaths.removeFrontendId(super.toString());
	}

}

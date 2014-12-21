package org.cryptomator.webdav.jackrabbit;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;

abstract class AbstractSessionAwareWebDavResourceFactory implements DavResourceFactory {

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		final DavSession session = request.getDavSession();
		if (session != null && session instanceof WebDavSession) {
			return createDavResource(locator, (WebDavSession) session, request, response);
		} else {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsupported session type.");
		}
	}

	protected abstract DavResource createDavResource(DavResourceLocator locator, WebDavSession session, DavServletRequest request, DavServletResponse response) throws DavException;

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
		if (session != null && session instanceof WebDavSession) {
			return createDavResource(locator, (WebDavSession) session);
		} else {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, "Unsupported session type.");
		}
	}

	protected abstract DavResource createDavResource(DavResourceLocator locator, WebDavSession session);

}

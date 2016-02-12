/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;

class DavSessionProviderImpl implements DavSessionProvider {

	@Override
	public boolean attachSession(WebdavRequest request) throws DavException {
		// every request gets a new session
		final DavSession session = new DavSessionImpl();
		session.addReference(request);
		request.setDavSession(session);
		return true;
	}

	@Override
	public void releaseSession(WebdavRequest request) {
		final DavSession session = request.getDavSession();
		if (session != null) {
			session.removeReference(request);
			request.setDavSession(null);
		}
	}

}

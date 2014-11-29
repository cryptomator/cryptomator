/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;

public class WebDavSessionProvider implements DavSessionProvider {

	@Override
	public boolean attachSession(WebdavRequest request) throws DavException {
		// every user gets a session
		request.setDavSession(new WebDavSession());
		return true;
	}

	@Override
	public void releaseSession(WebdavRequest request) {
		// do nothing
	}

}

/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.WebdavRequest;

class WebDavSession implements DavSession {

	private final WebdavRequest request;

	WebDavSession(WebdavRequest request) {
		this.request = request;
	}

	@Override
	public void addReference(Object reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeReference(Object reference) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addLockToken(String token) {
		// TODO Auto-generated method stub

	}

	@Override
	public String[] getLockTokens() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeLockToken(String token) {
		// TODO Auto-generated method stub

	}

	public WebdavRequest getRequest() {
		return request;
	}

}

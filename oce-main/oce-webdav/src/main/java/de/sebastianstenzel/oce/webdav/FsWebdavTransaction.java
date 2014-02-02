/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package de.sebastianstenzel.oce.webdav;

import java.security.Principal;

import org.xadisk.bridge.proxies.interfaces.Session;

import net.sf.webdav.ITransaction;

public class FsWebdavTransaction implements ITransaction {
	
	private final Principal principal;
	private final Session session;
	
	/**
	 * @param principal WebDAV User
	 * @param session XADisk Session
	 */
	FsWebdavTransaction(final Principal principal, final Session session) {
		this.principal = principal;
		this.session = session;
	}

	@Override
	public Principal getPrincipal() {
		return principal;
	}

	public Session getSession() {
		return session;
	}

}

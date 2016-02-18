/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.lock.AbstractActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;

class ExclusiveSharedLock extends AbstractActiveLock {

	private final String token;
	private final Type type;
	private final Scope scope;
	private String owner;
	private boolean isDeep = true; // deep by default
	private long expirationTime = DavConstants.INFINITE_TIMEOUT; // never expires by default;

	ExclusiveSharedLock(String token, LockInfo lockInfo) {
		this.token = token;
		this.type = lockInfo.getType();
		this.scope = lockInfo.getScope();
		this.owner = lockInfo.getOwner();
		this.isDeep = lockInfo.isDeep();
		setTimeout(lockInfo.getTimeout());
	}

	@Override
	public boolean isLockedByToken(String lockToken) {
		return token.equals(lockToken);
	}

	@Override
	public boolean isExpired() {
		return System.currentTimeMillis() > expirationTime;
	}

	@Override
	public String getToken() {
		return token;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	@Override
	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public long getTimeout() {
		return expirationTime - System.currentTimeMillis();
	}

	@Override
	public void setTimeout(long timeout) {
		if (timeout > 0) {
			expirationTime = System.currentTimeMillis() + timeout;
		}
	}

	@Override
	public boolean isDeep() {
		return isDeep;
	}

	@Override
	public void setIsDeep(boolean isDeep) {
		this.isDeep = isDeep;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Scope getScope() {
		return scope;
	}

	/* HASHCODE / EQUALS */

	@Override
	public int hashCode() {
		return getToken().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ExclusiveSharedLock) {
			ExclusiveSharedLock other = (ExclusiveSharedLock) obj;
			return this.getToken().equals(other.getToken());
		} else {
			return false;
		}
	}

}

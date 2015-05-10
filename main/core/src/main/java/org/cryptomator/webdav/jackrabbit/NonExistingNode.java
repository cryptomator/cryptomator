/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.cryptomator.crypto.Cryptor;

class NonExistingNode extends AbstractEncryptedNode {

	public NonExistingNode(CryptoResourceFactory factory, CryptoLocator locator, DavSession session, LockManager lockManager, Cryptor cryptor) {
		super(factory, locator, session, lockManager, cryptor);
	}

	@Override
	protected Path getPhysicalPath() {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public long getModificationTime() {
		return -1;
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

	@Override
	public void addMember(DavResource resource, InputContext inputContext) throws DavException {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

	@Override
	public DavResourceIterator getMembers() {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

	@Override
	public void removeMember(DavResource member) throws DavException {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

	@Override
	protected void determineProperties() {
		// do nothing.
	}

	@Override
	public void move(AbstractEncryptedNode destination) throws DavException {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

	@Override
	public void copy(AbstractEncryptedNode destination, boolean shallow) throws DavException {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

}

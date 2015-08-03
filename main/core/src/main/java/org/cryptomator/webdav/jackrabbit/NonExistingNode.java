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
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.webdav.jackrabbit.CryptoResourceFactory.NonExistingParentException;

class NonExistingNode extends AbstractEncryptedNode {

	public NonExistingNode(CryptoResourceFactory factory, DavResourceLocator locator, DavSession session, LockManager lockManager, Cryptor cryptor) {
		super(factory, locator, session, lockManager, cryptor, null);
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
	public void move(AbstractEncryptedNode destination) throws DavException {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

	@Override
	public void copy(AbstractEncryptedNode destination, boolean shallow) throws DavException {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

	@Override
	public void setProperty(DavProperty<?> property) throws DavException {
		throw new UnsupportedOperationException("Resource doesn't exist.");
	}

	/**
	 * @return lazily resolved file path, e.g. needed during MOVE operations.
	 */
	public Path materializeFilePath() {
		try {
			return factory.getEncryptedFilePath(locator.getResourcePath(), true);
		} catch (NonExistingParentException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @return lazily resolved directory file path, e.g. needed during MOVE operations.
	 */
	public Path materializeDirFilePath() {
		try {
			return factory.getEncryptedDirectoryFilePath(locator.getResourcePath(), true);
		} catch (NonExistingParentException e) {
			throw new IllegalStateException(e);
		}
	}

}

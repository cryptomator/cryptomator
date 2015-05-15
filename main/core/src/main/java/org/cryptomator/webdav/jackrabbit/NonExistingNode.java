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

class NonExistingNode extends AbstractEncryptedNode {

	private final Path filePath;
	private final Path dirFilePath;

	public NonExistingNode(CryptoResourceFactory factory, DavResourceLocator locator, DavSession session, LockManager lockManager, Cryptor cryptor, Path filePath, Path dirFilePath) {
		super(factory, locator, session, lockManager, cryptor, null);
		this.filePath = filePath;
		this.dirFilePath = dirFilePath;
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

	public Path getFilePath() {
		return filePath;
	}

	public Path getDirFilePath() {
		return dirFilePath;
	}

}

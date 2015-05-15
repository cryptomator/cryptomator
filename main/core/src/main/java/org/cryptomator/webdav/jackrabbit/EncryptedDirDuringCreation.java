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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.cryptomator.crypto.Cryptor;

class EncryptedDirDuringCreation extends AbstractEncryptedNode {

	private final Path parentDir;
	private final FilenameTranslator filenameTranslator;

	public EncryptedDirDuringCreation(CryptoResourceFactory factory, DavResourceLocator locator, DavSession session, LockManager lockManager, Cryptor cryptor, FilenameTranslator filenameTranslator, Path parentDir) {
		super(factory, locator, session, lockManager, cryptor);
		this.parentDir = parentDir;
		this.filenameTranslator = filenameTranslator;
	}

	public void doCreate() throws DavException {
		try {
			final String cleartextDirName = FilenameUtils.getName(locator.getResourcePath());
			final String ciphertextDirName = filenameTranslator.getEncryptedDirName(cleartextDirName);
			final Path dirFilePath = parentDir.resolve(ciphertextDirName);
			final String directoryId = UUID.randomUUID().toString();
			try (final FileChannel c = FileChannel.open(dirFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.DSYNC); final FileLock lock = c.lock()) {
				c.write(ByteBuffer.wrap(directoryId.getBytes(StandardCharsets.UTF_8)));
			} catch (FileAlreadyExistsException e) {
				throw new DavException(DavServletResponse.SC_METHOD_NOT_ALLOWED);
			}
			final Path directoryPath = filenameTranslator.getEncryptedDirectoryPath(directoryId);
			Files.createDirectories(directoryPath);
		} catch (IOException e) {
			throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
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
		return true;
	}

	@Override
	public long getModificationTime() {
		return Instant.now().toEpochMilli();
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

}

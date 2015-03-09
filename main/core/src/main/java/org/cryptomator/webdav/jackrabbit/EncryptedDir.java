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
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.cryptomator.crypto.Cryptor;
import org.cryptomator.webdav.exceptions.DavRuntimeException;
import org.cryptomator.webdav.exceptions.DecryptFailedRuntimeException;
import org.cryptomator.webdav.exceptions.IORuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EncryptedDir extends AbstractEncryptedNode {

	private static final Logger LOG = LoggerFactory.getLogger(EncryptedDir.class);

	public EncryptedDir(DavResourceFactory factory, DavResourceLocator locator, DavSession session, LockManager lockManager, Cryptor cryptor) {
		super(factory, locator, session, lockManager, cryptor);
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public void addMember(DavResource resource, InputContext inputContext) throws DavException {
		if (resource.isCollection()) {
			this.addMemberDir(resource, inputContext);
		} else {
			this.addMemberFile(resource, inputContext);
		}
	}

	private void addMemberDir(DavResource resource, InputContext inputContext) throws DavException {
		final Path childPath = ResourcePathUtils.getPhysicalPath(resource);
		try {
			Files.createDirectories(childPath);
		} catch (SecurityException e) {
			throw new DavException(DavServletResponse.SC_FORBIDDEN, e);
		} catch (IOException e) {
			LOG.error("Failed to create subdirectory.", e);
			throw new IORuntimeException(e);
		}
	}

	private void addMemberFile(DavResource resource, InputContext inputContext) throws DavException {
		final Path childPath = ResourcePathUtils.getPhysicalPath(resource);
		try (final SeekableByteChannel channel = Files.newByteChannel(childPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			cryptor.encryptFile(inputContext.getInputStream(), channel);
		} catch (SecurityException e) {
			throw new DavException(DavServletResponse.SC_FORBIDDEN, e);
		} catch (IOException e) {
			LOG.error("Failed to create file.", e);
			throw new IORuntimeException(e);
		} finally {
			IOUtils.closeQuietly(inputContext.getInputStream());
		}
	}

	@Override
	public DavResourceIterator getMembers() {
		final Path dir = ResourcePathUtils.getPhysicalPath(this);
		try {
			final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir, cryptor.getPayloadFilesFilter());
			final List<DavResource> result = new ArrayList<>();

			for (final Path childPath : directoryStream) {
				try {
					final DavResourceLocator childLocator = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), childPath.toString(), false);
					final DavResource resource = factory.createResource(childLocator, session);
					result.add(resource);
				} catch (DecryptFailedRuntimeException e) {
					LOG.warn("Decryption of resource failed: " + childPath);
					continue;
				}
			}
			return new DavResourceIteratorImpl(result);
		} catch (IOException e) {
			LOG.error("Exception during getMembers.", e);
			throw new IORuntimeException(e);
		} catch (DavException e) {
			LOG.error("Exception during getMembers.", e);
			throw new DavRuntimeException(e);
		}
	}

	@Override
	public void removeMember(DavResource member) throws DavException {
		final Path memberPath = ResourcePathUtils.getPhysicalPath(member);
		try {
			if (Files.exists(memberPath)) {
				Files.walkFileTree(memberPath, new DeletingFileVisitor());
			}
		} catch (SecurityException e) {
			throw new DavException(DavServletResponse.SC_FORBIDDEN, e);
		} catch (IOException e) {
			throw new IORuntimeException(e);
		}
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		// do nothing
	}

	@Override
	protected void determineProperties() {
		final Path path = ResourcePathUtils.getPhysicalPath(this);
		properties.add(new ResourceType(ResourceType.COLLECTION));
		properties.add(new DefaultDavProperty<Integer>(DavPropertyName.ISCOLLECTION, 1));
		if (Files.exists(path)) {
			try {
				final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
				properties.add(new DefaultDavProperty<String>(DavPropertyName.CREATIONDATE, FileTimeUtils.toRfc1123String(attrs.creationTime())));
				properties.add(new DefaultDavProperty<String>(DavPropertyName.GETLASTMODIFIED, FileTimeUtils.toRfc1123String(attrs.lastModifiedTime())));
			} catch (IOException e) {
				LOG.error("Error determining metadata " + path.toString(), e);
				// don't add any further properties
			}
		}
	}

	/**
	 * Deletes all files and folders, it visits.
	 */
	private static class DeletingFileVisitor extends SimpleFileVisitor<Path> {

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
			if (attributes.isRegularFile()) {
				Files.delete(file);
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			Files.delete(dir);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			LOG.error("Failed to delete file " + file.toString(), exc);
			return FileVisitResult.TERMINATE;
		}

	}

}

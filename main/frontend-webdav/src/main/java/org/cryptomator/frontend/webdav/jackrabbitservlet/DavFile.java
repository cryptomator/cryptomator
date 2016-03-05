/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.Optional;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.jackrabbit.FileLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

class DavFile extends DavNode<FileLocator> {

	private static final Logger LOG = LoggerFactory.getLogger(DavFile.class);

	public DavFile(FilesystemResourceFactory factory, LockManager lockManager, DavSession session, FileLocator node) {
		super(factory, lockManager, session, node);
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		outputContext.setModificationTime(node.lastModified().toEpochMilli());
		if (!outputContext.hasStream()) {
			return;
		}
		try (ReadableFile src = node.openReadable(); WritableByteChannel dst = Channels.newChannel(outputContext.getOutputStream())) {
			outputContext.setContentLength(src.size());
			ByteStreams.copy(src, dst);
		}
	}

	@Override
	public void addMember(DavResource resource, InputContext inputContext) throws DavException {
		throw new UnsupportedOperationException();
	}

	@Override
	public DavResourceIterator getMembers() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeMember(DavResource member) throws DavException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void move(DavResource destination) throws DavException {
		if (destination instanceof DavFile) {
			DavFile dst = (DavFile) destination;
			if (dst.node.exists()) {
				// Overwrite header already checked by AbstractWebdavServlet#validateDestination
				dst.node.delete();
			} else if (!dst.node.parent().get().exists()) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "Destination's parent doesn't exist.");
			}
			node.moveTo(dst.node);
		} else if (destination instanceof DavFolder) {
			DavFolder dst = (DavFolder) destination;
			Folder parent = dst.node.parent().get();
			File newDst = parent.file(dst.node.name());
			if (dst.node.exists()) {
				dst.node.delete();
			} else if (!parent.exists()) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "Destination's parent doesn't exist.");
			}
			node.moveTo(newDst);
		} else {
			throw new IllegalArgumentException("Destination not a DavFolder: " + destination.getClass().getName());
		}
	}

	@Override
	public void copy(DavResource destination, boolean shallow) throws DavException {
		if (destination instanceof DavFile) {
			DavFile dst = (DavFile) destination;
			if (dst.node.exists()) {
				// Overwrite header already checked by AbstractWebdavServlet#validateDestination
				dst.node.delete();
			} else if (!dst.node.parent().get().exists()) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "Destination's parent doesn't exist.");
			}
			node.copyTo(dst.node);
		} else if (destination instanceof DavFolder) {
			DavFolder dst = (DavFolder) destination;
			Folder parent = dst.node.parent().get();
			File newDst = parent.file(dst.node.name());
			if (dst.node.exists()) {
				dst.node.delete();
			} else if (!parent.exists()) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "Destination's parent doesn't exist.");
			}
			node.copyTo(newDst);
		} else {
			throw new IllegalArgumentException("Destination not a DavFile: " + destination.getClass().getName());
		}
	}

	@Override
	public DavProperty<?> getProperty(DavPropertyName name) {
		if (DavPropertyName.GETCONTENTLENGTH.equals(name)) {
			return sizeProperty().orElse(null);
		} else {
			return super.getProperty(name);
		}
	}

	@Override
	public DavPropertySet getProperties() {
		final DavPropertySet result = super.getProperties();
		if (!result.contains(DavPropertyName.GETCONTENTLENGTH)) {
			sizeProperty().ifPresent(result::add);
		}
		return result;
	}

	private Optional<DavProperty<?>> sizeProperty() {
		if (node.exists()) {
			try (ReadableFile src = node.openReadable()) {
				return Optional.of(new DefaultDavProperty<Long>(DavPropertyName.GETCONTENTLENGTH, src.size()));
			} catch (RuntimeException e) {
				LOG.warn("Could not determine file size of " + getResourcePath(), e);
				return Optional.empty();
			}
		} else {
			return Optional.empty();
		}
	}

	@Override
	protected void setModificationTime(Instant lastModified) {
		node.setLastModified(lastModified);
	}

	@Override
	protected void setCreationTime(Instant creationTime) {
		node.setCreationTime(creationTime);
	}

	@Override
	public ActiveLock lock(LockInfo reqLockInfo) throws DavException {
		ActiveLock lock = super.lock(reqLockInfo);
		if (!exists()) {
			DavFolder parentFolder = getCollection();
			assert parentFolder != null : "File always has a folder.";
			parentFolder.addMember(this, new NullInputContext());
		}
		return lock;
	}

}

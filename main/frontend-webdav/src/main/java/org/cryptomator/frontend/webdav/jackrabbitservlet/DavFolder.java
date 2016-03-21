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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.cryptomator.common.streams.AutoClosingStream;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.jackrabbit.FileLocator;
import org.cryptomator.filesystem.jackrabbit.FolderLocator;

import com.google.common.io.ByteStreams;

class DavFolder extends DavNode<FolderLocator> {

	private static final DavPropertyName PROPERTY_QUOTA_AVAILABLE = DavPropertyName.create("quota-available-bytes");
	private static final DavPropertyName PROPERTY_QUOTA_USED = DavPropertyName.create("quota-used-bytes");

	public DavFolder(FilesystemResourceFactory factory, LockManager lockManager, DavSession session, FolderLocator folder) {
		super(factory, lockManager, session, folder);
		properties.add(new ResourceType(ResourceType.COLLECTION));
		properties.add(new DefaultDavProperty<Integer>(DavPropertyName.ISCOLLECTION, 1));
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		// no-op
	}

	@Override
	public void addMember(DavResource resource, InputContext inputContext) throws DavException {
		if (resource instanceof DavFolder) {
			addMemberFolder((DavFolder) resource);
		} else if (resource instanceof DavFile) {
			assert inputContext.hasStream();
			addMemberFile((DavFile) resource, inputContext.getInputStream());
		} else {
			throw new IllegalArgumentException("Unsupported resource type: " + resource.getClass().getName());
		}
	}

	private void addMemberFolder(DavFolder memberFolder) {
		node.folder(memberFolder.getDisplayName()).create();
	}

	private void addMemberFile(DavFile memberFile, InputStream inputStream) {
		try (ReadableByteChannel src = Channels.newChannel(inputStream); WritableFile dst = node.file(memberFile.getDisplayName()).openWritable()) {
			dst.truncate();
			ByteStreams.copy(src, dst);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public DavResourceIterator getMembers() {
		final Stream<DavFolder> folders = node.folders().map(this::folderToDavFolder);
		final Stream<DavFile> files = node.files().map(this::fileToDavFile);
		final Stream<DavResource> members = AutoClosingStream.from(Stream.concat(folders, files));
		return new DavResourceIteratorImpl(members.collect(Collectors.toList()));
	}

	private DavFolder folderToDavFolder(FolderLocator memberFolder) {
		return factory.createFolder(memberFolder, session);
	}

	private DavFile fileToDavFile(FileLocator memberFile) {
		return factory.createFile(memberFile, session);
	}

	@Override
	public void removeMember(DavResource member) throws DavException {
		for (ActiveLock lock : member.getLocks()) {
			member.unlock(lock.getToken());
		}
		final Node child = getMemberNode(member.getDisplayName());
		child.delete();
	}

	/**
	 * @throws DavException
	 *             Error 404 if no child with the given name exists
	 */
	private Node getMemberNode(String name) throws DavException {
		Node file = node.file(name);
		Node folder = node.folder(name);
		if (file.exists()) {
			return file;
		} else if (folder.exists()) {
			return folder;
		} else {
			throw new DavException(DavServletResponse.SC_NOT_FOUND, "No such file or directory: " + node.getResourcePath() + name);
		}
	}

	@Override
	public void move(DavResource destination) throws DavException {
		if (destination instanceof DavFolder) {
			DavFolder dst = (DavFolder) destination;
			if (dst.node.exists()) {
				dst.node.delete();
			} else if (!dst.node.parent().get().exists()) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "Destination's parent doesn't exist.");
			}
			node.moveTo(dst.node);
		} else if (destination instanceof DavFile) {
			DavFile dst = (DavFile) destination;
			Folder parent = dst.node.parent().get();
			Folder newDst = parent.folder(dst.node.name());
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
		if (!node.exists()) {
			throw new DavException(DavServletResponse.SC_NOT_FOUND);
		}
		if (destination instanceof DavNode) {
			DavNode<?> dst = (DavNode<?>) destination;
			if (!dst.node.parent().get().exists()) {
				throw new DavException(DavServletResponse.SC_CONFLICT, "Destination's parent doesn't exist.");
			}
		}
		if (destination instanceof DavFolder) {
			DavFolder dst = (DavFolder) destination;
			if (shallow) {
				// create destination, if it doesn't exist yet:
				dst.node.create();
				// http://www.webdav.org/specs/rfc2518.html#copy.for.collections
				node.creationTime().ifPresent(dst::setCreationTime);
				dst.setModificationTime(node.lastModified());
			} else {
				node.copyTo(dst.node);
			}
		} else if (destination instanceof DavFile) {
			DavFile dst = (DavFile) destination;
			Folder parent = dst.node.parent().get();
			Folder newDst = parent.folder(dst.node.name());
			if (dst.node.exists()) {
				dst.node.delete();
			}
			node.copyTo(newDst);
		} else {
			throw new IllegalArgumentException("Destination not a DavFolder: " + destination.getClass().getName());
		}
	}

	@Override
	protected void setModificationTime(Instant instant) {
		node.setLastModified(instant);
	}

	@Override
	protected void setCreationTime(Instant instant) {
		node.setCreationTime(instant);
	}

	@Override
	public DavPropertyName[] getPropertyNames() {
		return ArrayUtils.addAll(super.getPropertyNames(), PROPERTY_QUOTA_AVAILABLE, PROPERTY_QUOTA_USED);
	}

	@Override
	public DavProperty<?> getProperty(DavPropertyName name) {
		if (PROPERTY_QUOTA_AVAILABLE.equals(name)) {
			return node.fileSystem().quotaAvailableBytes().map(numBytes -> new DefaultDavProperty<Long>(name, numBytes)).orElse(null);
		} else if (PROPERTY_QUOTA_USED.equals(name)) {
			return node.fileSystem().quotaUsedBytes().map(numBytes -> new DefaultDavProperty<Long>(name, numBytes)).orElse(null);
		} else {
			return super.getProperty(name);
		}
	}

}

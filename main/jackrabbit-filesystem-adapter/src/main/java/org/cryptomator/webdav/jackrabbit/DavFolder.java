/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.webdav.jackrabbit.DavPathFactory.DavPath;

class DavFolder extends DavNode<Folder> {

	public DavFolder(FilesystemResourceFactory factory, LockManager lockManager, DavSession session, DavPath path, Folder folder) {
		super(factory, lockManager, session, path, folder);
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
			addMemberFile((DavFile) resource, inputContext.getInputStream());
		} else {
			throw new IllegalArgumentException("Unsupported resource type: " + resource.getClass().getName());
		}
	}

	private void addMemberFolder(DavFolder memberFolder) {
		node.folder(memberFolder.getDisplayName()).create(FolderCreateMode.FAIL_IF_PARENT_IS_MISSING);
	}

	private void addMemberFile(DavFile memberFile, InputStream inputStream) {
		try (ReadableByteChannel src = Channels.newChannel(inputStream); WritableFile dst = node.file(memberFile.getDisplayName()).openWritable()) {
			ByteBuffer buf = ByteBuffer.allocate(1337);
			while (src.read(buf) != -1) {
				buf.flip();
				dst.write(buf);
				buf.clear();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public DavResourceIterator getMembers() {
		final Stream<DavFolder> folders = node.folders().map(this::getMemberFolder);
		final Stream<DavFile> files = node.files().map(this::getMemberFile);
		return new DavResourceIteratorImpl(Stream.concat(folders, files).collect(Collectors.toList()));
	}

	private DavFolder getMemberFolder(Folder memberFolder) {
		final DavPath subFolderLocator = path.getChild(memberFolder.name() + '/');
		return factory.createFolder(memberFolder, subFolderLocator, session);
	}

	private DavFile getMemberFile(File memberFile) {
		final DavPath subFolderLocator = path.getChild(memberFile.name());
		return factory.createFile(memberFile, subFolderLocator, session);
	}

	@Override
	public void removeMember(DavResource member) throws DavException {
		// TODO Auto-generated method stub

	}

	@Override
	public void move(DavResource destination) throws DavException {
		// TODO Auto-generated method stub

	}

	@Override
	public void copy(DavResource destination, boolean shallow) throws DavException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setModificationTime(Instant instant) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setCreationTime(Instant instant) {
		// TODO Auto-generated method stub

	}

}

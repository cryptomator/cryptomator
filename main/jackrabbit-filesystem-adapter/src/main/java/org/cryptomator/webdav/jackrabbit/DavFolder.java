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
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;

class DavFolder extends DavNode<Folder> {

	public DavFolder(FilesystemResourceFactory factory, LockManager lockManager, DavSession session, DavResourceLocator locator, Folder folder) {
		super(factory, lockManager, session, locator, folder);
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
		// TODO Auto-generated method stub

	}

	@Override
	public DavResourceIterator getMembers() {
		final Stream<DavFolder> folders = node.folders().map(this::getMemberFolder);
		final Stream<DavFile> files = node.files().map(this::getMemberFile);
		return new DavResourceIteratorImpl(Stream.concat(folders, files).collect(Collectors.toList()));
	}

	private DavFolder getMemberFolder(Folder memberFolder) {
		final String subFolderResourcePath = locator.getResourcePath() + memberFolder.name() + '/';
		final DavResourceLocator subFolderLocator = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), subFolderResourcePath);
		return factory.createFolder(memberFolder, subFolderLocator, session);
	}

	private DavFile getMemberFile(File memberFile) {
		final String subFolderResourcePath = locator.getResourcePath() + memberFile.name();
		final DavResourceLocator subFolderLocator = locator.getFactory().createResourceLocator(locator.getPrefix(), locator.getWorkspacePath(), subFolderResourcePath);
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

/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.webdav.jackrabbitservlet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.jackrabbit.FileLocator;

class DavFile extends DavNode<FileLocator> {

	private static final int BUFFER_SIZE = 32 * 1024;

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
			ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
			do {
				buf.clear();
				src.read(buf);
				buf.flip();
			} while (dst.write(buf) > 0);
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
			node.moveTo(dst.node);
		} else {
			throw new IllegalArgumentException("Destination not a DavFolder: " + destination.getClass().getName());
		}
	}

	@Override
	public void copy(DavResource destination, boolean shallow) throws DavException {
		if (destination instanceof DavFile) {
			DavFile dst = (DavFile) destination;
			node.copyTo(dst.node);
		} else {
			throw new IllegalArgumentException("Destination not a DavFolder: " + destination.getClass().getName());
		}
	}

	@Override
	public DavProperty<?> getProperty(DavPropertyName name) {
		if (DavPropertyName.GETCONTENTLENGTH.equals(name) && node.exists()) {
			try (ReadableFile src = node.openReadable()) {
				return new DefaultDavProperty<Long>(DavPropertyName.GETCONTENTLENGTH, src.size());
			}
		} else {
			return super.getProperty(name);
		}
	}

	@Override
	public DavPropertySet getProperties() {
		final DavPropertySet result = super.getProperties();
		if (node.exists()) {
			try (ReadableFile src = node.openReadable()) {
				result.add(new DefaultDavProperty<Long>(DavPropertyName.GETCONTENTLENGTH, src.size()));
			} catch (UncheckedIOException e) {
				result.add(new DefaultDavProperty<Long>(DavPropertyName.GETCONTENTLENGTH, -1l));
			}
		}
		return result;
	}

	@Override
	protected void setModificationTime(Instant instant) {
		try (WritableFile writable = node.openWritable()) {
			writable.setLastModified(instant);
		}
	}

	@Override
	protected void setCreationTime(Instant instant) {
		try (WritableFile writable = node.openWritable()) {
			writable.setCreationTime(instant);
		}
	}

}

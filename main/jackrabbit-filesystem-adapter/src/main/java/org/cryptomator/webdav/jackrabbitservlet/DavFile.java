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
import org.cryptomator.filesystem.FileSystemFeature;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.filesystem.jackrabbit.FileLocator;

class DavFile extends DavNode<FileLocator> {

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
			// TODO filesize before sending content
			outputContext.setContentLength(-1l);
			ByteBuffer buf = ByteBuffer.allocate(1337);
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
	protected void setModificationTime(Instant instant) {
		try (WritableFile writable = node.openWritable()) {
			writable.setLastModified(instant);
		}
	}

	@Override
	protected void setCreationTime(Instant instant) {
		if (node.fileSystem().supports(FileSystemFeature.CREATION_TIME_FEATURE)) {
			try (WritableFile writable = node.openWritable()) {
				writable.setCreationTime(instant);
			}
		}
	}

}

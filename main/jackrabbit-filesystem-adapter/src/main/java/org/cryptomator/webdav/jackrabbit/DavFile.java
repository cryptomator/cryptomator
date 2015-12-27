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
import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.webdav.jackrabbit.DavPathFactory.DavPath;

class DavFile extends DavNode<File> {

	public DavFile(FilesystemResourceFactory factory, LockManager lockManager, DavSession session, DavPath path, File node) {
		super(factory, lockManager, session, path, node);
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
		// TODO Auto-generated method stub

	}

	@Override
	public void copy(DavResource destination, boolean shallow) throws DavException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setModificationTime(Instant instant) {
		try (WritableFile writable = node.openWritable()) {
			writable.setLastModified(instant);
		}
	}

	@Override
	protected void setCreationTime(Instant instant) {
		// TODO Auto-generated method stub

	}

}

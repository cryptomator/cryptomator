/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.jackrabbit.FileLocator;
import org.eclipse.jetty.http.HttpHeader;

import com.google.common.io.ByteStreams;

/**
 * Sends the full file in reaction to an unsatisfiable range.
 * 
 * @see {@link https://tools.ietf.org/html/rfc7233#section-4.2}
 */
class DavFileWithUnsatisfiableRange extends DavFile {

	public DavFileWithUnsatisfiableRange(FilesystemResourceFactory factory, LockManager lockManager, DavSession session, FileLocator node) throws DavException {
		super(factory, lockManager, session, node);
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		outputContext.setModificationTime(node.lastModified().toEpochMilli());
		if (!outputContext.hasStream()) {
			return;
		}
		try (ReadableFile src = node.openReadable(); OutputStream out = outputContext.getOutputStream()) {
			final long contentLength = src.size();
			outputContext.setContentLength(contentLength);
			outputContext.setProperty(HttpHeader.CONTENT_RANGE.asString(), "bytes */" + contentLength);
			ByteStreams.copy(src, Channels.newChannel(out));
		}
	}

}

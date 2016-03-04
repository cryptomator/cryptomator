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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.Objects;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.jackrabbit.FileLocator;
import org.eclipse.jetty.http.HttpHeader;

import com.google.common.io.ByteStreams;

/**
 * Delivers only the requested range of bytes from a file.
 * 
 * @see {@link https://tools.ietf.org/html/rfc7233#section-4}
 */
class DavFileWithRange extends DavFile {

	private final Pair<String, String> requestRange;

	public DavFileWithRange(FilesystemResourceFactory factory, LockManager lockManager, DavSession session, FileLocator node, Pair<String, String> requestRange) throws DavException {
		super(factory, lockManager, session, node);
		this.requestRange = Objects.requireNonNull(requestRange);
	}

	@Override
	public void spool(OutputContext outputContext) throws IOException {
		outputContext.setModificationTime(node.lastModified().toEpochMilli());
		if (!outputContext.hasStream()) {
			return;
		}
		try (ReadableFile src = node.openReadable(); OutputStream out = outputContext.getOutputStream()) {
			final long contentLength = src.size();
			final Pair<Long, Long> range = getEffectiveRange(contentLength);
			if (range.getLeft() < 0 || range.getLeft() > range.getRight() || range.getRight() > contentLength) {
				outputContext.setProperty(HttpHeader.CONTENT_RANGE.asString(), "bytes */" + contentLength);
				throw new UncheckedDavException(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "Valid Range would be in [0, " + contentLength + "]");
			}
			final Long rangeLength = range.getRight() - range.getLeft() + 1;
			outputContext.setContentLength(rangeLength);
			outputContext.setProperty(HttpHeader.CONTENT_RANGE.asString(), contentRangeResponseHeader(range.getLeft(), range.getRight(), contentLength));
			src.position(range.getLeft());
			InputStream limitedIn = ByteStreams.limit(Channels.newInputStream(src), rangeLength);
			ByteStreams.copy(limitedIn, out);
		}
	}

	private String contentRangeResponseHeader(long firstByte, long lastByte, long completeLength) {
		return String.format("bytes %d-%d/%d", firstByte, lastByte, completeLength);
	}

	private Pair<Long, Long> getEffectiveRange(long contentLength) {
		try {
			final Long lower = requestRange.getLeft().isEmpty() ? null : Long.valueOf(requestRange.getLeft());
			final Long upper = requestRange.getRight().isEmpty() ? null : Long.valueOf(requestRange.getRight());
			if (lower == null && upper == null) {
				return new ImmutablePair<Long, Long>(0l, contentLength - 1);
			} else if (lower == null) {
				return new ImmutablePair<Long, Long>(contentLength - upper, contentLength - 1);
			} else if (upper == null) {
				return new ImmutablePair<Long, Long>(lower, contentLength - 1);
			} else {
				return new ImmutablePair<Long, Long>(lower, Math.min(upper, contentLength - 1));
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid byte range: " + requestRange, e);
		}
	}

}

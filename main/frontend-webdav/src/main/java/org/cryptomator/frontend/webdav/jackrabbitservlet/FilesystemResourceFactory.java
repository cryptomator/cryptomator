/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.cryptomator.filesystem.jackrabbit.FileLocator;
import org.cryptomator.filesystem.jackrabbit.FolderLocator;
import org.eclipse.jetty.http.HttpHeader;

class FilesystemResourceFactory implements DavResourceFactory {

	private static final String RANGE_BYTE_PREFIX = "bytes=";
	private static final char RANGE_SET_SEP = ',';
	private static final char RANGE_SEP = '-';

	private final LockManager lockManager;

	public FilesystemResourceFactory() {
		this.lockManager = new ExclusiveSharedLockManager();
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
		if (locator instanceof FileLocator && DavMethods.METHOD_GET.equals(request.getMethod()) && request.getHeader(HttpHeader.RANGE.asString()) != null) {
			return createFileRange((FileLocator) locator, request.getDavSession(), request, response);
		} else {
			return createResource(locator, request.getDavSession());
		}
	}

	@Override
	public DavResource createResource(DavResourceLocator locator, DavSession session) {
		if (locator instanceof FolderLocator) {
			FolderLocator folder = (FolderLocator) locator;
			return createFolder(folder, session);
		} else if (locator instanceof FileLocator) {
			FileLocator file = (FileLocator) locator;
			return createFile(file, session);
		} else {
			throw new IllegalArgumentException("Unsupported locator type " + locator.getClass().getName());
		}
	}

	DavFolder createFolder(FolderLocator folder, DavSession session) {
		return new DavFolder(this, lockManager, session, folder);
	}

	DavFile createFile(FileLocator file, DavSession session) {
		return new DavFile(this, lockManager, session, file);
	}

	private DavFile createFileRange(FileLocator file, DavSession session, DavServletRequest request, DavServletResponse response) throws DavException {
		// 404 for non-existing resources:
		if (!file.exists()) {
			throw new DavException(DavServletResponse.SC_NOT_FOUND);
		}

		// 200 for "normal" resources, if if-range is not satisified:
		final String ifRangeHeader = request.getHeader(HttpHeader.IF_RANGE.asString());
		if (!isIfRangeHeaderSatisfied(file, ifRangeHeader)) {
			return createFile(file, session);
		}

		final String rangeHeader = request.getHeader(HttpHeader.RANGE.asString());
		try {
			// 206 for ranged resources:
			final Pair<String, String> parsedRange = parseRangeRequestHeader(rangeHeader);
			response.setStatus(DavServletResponse.SC_PARTIAL_CONTENT);
			return new DavFileWithRange(this, lockManager, session, file, parsedRange);
		} catch (DavException ex) {
			if (ex.getErrorCode() == DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE) {
				// 416 for unsatisfiable ranges:
				response.setStatus(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return new DavFileWithUnsatisfiableRange(this, lockManager, session, file);
			} else {
				throw new DavException(ex.getErrorCode(), ex);
			}
		}
	}

	/**
	 * Processes the given range header field, if it is supported. Only headers containing a single byte range are supported.<br/>
	 * <code>
	 * bytes=100-200<br/>
	 * bytes=-500<br/>
	 * bytes=1000-
	 * </code>
	 * 
	 * @return Tuple of lower and upper range.
	 * @throws DavException HTTP statuscode 400 for malformed requests. 416 if requested range is not supported.
	 */
	private Pair<String, String> parseRangeRequestHeader(String rangeHeader) throws DavException {
		assert rangeHeader != null;
		if (!rangeHeader.startsWith(RANGE_BYTE_PREFIX)) {
			throw new DavException(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
		}
		final String byteRangeSet = StringUtils.removeStartIgnoreCase(rangeHeader, RANGE_BYTE_PREFIX);
		final String[] byteRanges = StringUtils.split(byteRangeSet, RANGE_SET_SEP);
		if (byteRanges.length != 1) {
			throw new DavException(DavServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
		}
		final String byteRange = byteRanges[0];
		final String[] bytePos = StringUtils.splitPreserveAllTokens(byteRange, RANGE_SEP);
		if (bytePos.length != 2 || bytePos[0].isEmpty() && bytePos[1].isEmpty()) {
			throw new DavException(DavServletResponse.SC_BAD_REQUEST, "malformed range header: " + rangeHeader);
		}
		return new ImmutablePair<>(bytePos[0], bytePos[1]);
	}

	/**
	 * @return <code>true</code> if a partial response should be generated according to an If-Range precondition.
	 */
	private boolean isIfRangeHeaderSatisfied(FileLocator file, String ifRangeHeader) throws DavException {
		if (ifRangeHeader == null) {
			// no header set -> satisfied implicitly
			return true;
		} else {
			try {
				Instant expectedTime = Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(ifRangeHeader));
				Instant actualTime = file.lastModified();
				return expectedTime.compareTo(actualTime) == 0;
			} catch (DateTimeParseException e) {
				throw new DavException(DavServletResponse.SC_BAD_REQUEST, "Unsupported If-Range header: " + ifRangeHeader);
			}
		}
	}

}

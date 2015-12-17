/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.UncheckedIOException;
import java.time.Instant;

public interface WritableFile extends WritableBytes, AutoCloseable {

	void moveTo(WritableFile other) throws UncheckedIOException;

	void setLastModified(Instant instant) throws UncheckedIOException;

	/**
	 * <p>
	 * Deletes this file from the file system.
	 * <p>
	 * Deleting a file causes it to be {@link WritableFile#close() closed}.
	 */
	void delete() throws UncheckedIOException;

	void truncate() throws UncheckedIOException;

	/**
	 * <p>
	 * Closes this {@code WritableFile} which finally commits all operations
	 * performed on it to the underlying file system.
	 * <p>
	 * After a {@code WritableFile} has been closed all other operations will
	 * throw an {@link UncheckedIOException}.
	 * <p>
	 * Invoking this method on a {@link WritableFile} which has already been
	 * closed does nothing.
	 */
	@Override
	void close() throws UncheckedIOException;

}

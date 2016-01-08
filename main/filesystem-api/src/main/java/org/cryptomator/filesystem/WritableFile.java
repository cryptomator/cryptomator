/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;

public interface WritableFile extends WritableByteChannel {

	/**
	 * <p>
	 * Moves this file including content to another.
	 * <p>
	 * Moving a file causes itself and the target to be
	 * {@link WritableFile#close() closed}.
	 */
	void moveTo(WritableFile other) throws UncheckedIOException;

	void setLastModified(Instant instant) throws UncheckedIOException;

	/**
	 * <p>
	 * Sets the creation time of the file.
	 * <p>
	 * Setting the creation time may not be supported by all {@link FileSystem FileSystems}. If the {@code FileSystem} this {@code WritableFile} belongs to does not support the
	 * {@link FileSystemFeature#CREATION_TIME_FEATURE} the behavior of this method is unspecified.
	 * 
	 * @param instant the time to set as creation time
	 * @see FileSystem#supports(Class)
	 */
	default void setCreationTime(Instant instant) throws UncheckedIOException {
		throw new UncheckedIOException(new IOException("CreationTime not supported"));
	}

	/**
	 * <p>
	 * Deletes this file from the file system.
	 * <p>
	 * Deleting a file causes it to be {@link WritableFile#close() closed}.
	 */
	void delete() throws UncheckedIOException;

	void truncate() throws UncheckedIOException;

	/**
	 * Writes the data in the given byte buffer to this readable bytes at the
	 * current position.
	 * 
	 * @param source
	 *            the byte buffer to use
	 * @return the number of bytes written, always equal to
	 *         {@code source.remaining()}
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs while writing
	 */
	int write(ByteBuffer source) throws UncheckedIOException;

	/**
	 * <p>
	 * Fast-forwards or rewinds the file to the specified position.
	 * <p>
	 * Consecutive writes on the file will begin at the new position.
	 * <p>
	 * If the position is set to a value greater than the current end of file
	 * consecutive writes will write data to the given position. The value of
	 * all bytes between this position and the previous end of file will be
	 * unspecified.
	 * 
	 * @param position
	 *            the position to set the file to
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs
	 */
	void position(long position) throws UncheckedIOException;

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

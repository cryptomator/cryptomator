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

public interface WritableFile extends WritableByteChannel {

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
	@Override
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

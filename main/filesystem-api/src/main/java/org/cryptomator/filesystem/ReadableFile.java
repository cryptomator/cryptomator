/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public interface ReadableFile extends ReadableByteChannel {

	/**
	 * <p>
	 * Tries to fill the remaining space in the given byte buffer with data from
	 * this readable bytes from the current position.
	 * <p>
	 * May read less bytes if the end of this readable bytes has been reached.
	 * 
	 * @param target
	 *            the byte buffer to fill
	 * @return the number of bytes actually read, or {@code -1} if the end of
	 *         file has been reached
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs while reading from this
	 *             {@code ReadableBytes}
	 */
	@Override
	int read(ByteBuffer target) throws UncheckedIOException;

	/**
	 * @return The current size of the file. This value is a snapshot and might have been changed by concurrent modifications.
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs
	 */
	long size() throws UncheckedIOException;

	/**
	 * <p>
	 * Fast-forwards or rewinds the file to the specified position.
	 * <p>
	 * Consecutive reads on the file will begin at the new position.
	 * 
	 * @param position
	 *            the position to set the file to
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs
	 * 
	 */
	void position(long position) throws UncheckedIOException;

	@Override
	void close() throws UncheckedIOException;

}

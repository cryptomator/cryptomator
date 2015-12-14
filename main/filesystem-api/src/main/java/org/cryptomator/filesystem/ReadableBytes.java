/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

public interface ReadableBytes {

	/**
	 * <p>
	 * Tries to fill the remaining space in the given byte buffer with data from
	 * this readable bytes from the current position.
	 * <p>
	 * May read less bytes if the end of this readable bytes has been reached.
	 * 
	 * @param target
	 *            the byte buffer to fill
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs while reading from this
	 *             {@code ReadableBytes}
	 */
	void read(ByteBuffer target) throws UncheckedIOException;

	/**
	 * <p>
	 * Tries to fill the remaining space in the given byte buffer with data from
	 * this readable bytes from the given position.
	 * <p>
	 * May read less bytes if the end of this readable bytes has been reached.
	 * 
	 * @param target
	 *            the byte buffer to fill
	 * @param position
	 *            the position to read bytes from
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs while reading from this
	 *             {@code ReadableBytes}
	 */
	void read(ByteBuffer target, int position) throws UncheckedIOException;

}

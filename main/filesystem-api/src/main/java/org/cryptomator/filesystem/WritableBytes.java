/*******************************************************************************
 * Copyright (c) 2015 Markus Kreusch
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 ******************************************************************************/
package org.cryptomator.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

public interface WritableBytes {

	/**
	 * Writes the data in the given byte buffer to this readable bytes at the
	 * current position.
	 * 
	 * @param target
	 *            the byte buffer to use
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs while writing
	 */
	void write(ByteBuffer source) throws UncheckedIOException;

	/**
	 * Writes the data in the given byte buffer to this readable bytes at the
	 * given position, overwriting existing content (not inserting).
	 * 
	 * @param target
	 *            the byte buffer to use
	 * @param position
	 *            the position to write the data to
	 * @throws UncheckedIOException
	 *             if an {@link IOException} occurs while writing
	 */
	void write(ByteBuffer source, int position) throws UncheckedIOException;

}

/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.io;

import java.nio.ByteBuffer;

public final class ByteBuffers {

	private ByteBuffers() {
	}

	/**
	 * Copies as many bytes as possible from the given source to the destination buffer.
	 * The position of both buffers will be incremented by as many bytes as have been copied.
	 * 
	 * @param source ByteBuffer from which bytes are read
	 * @param destination ByteBuffer into which bytes are written
	 * @return number of bytes copied, i.e. {@link ByteBuffer#remaining() source.remaining()} or {@link ByteBuffer#remaining() destination.remaining()}, whatever is less.
	 */
	public static int copy(ByteBuffer source, ByteBuffer destination) {
		final int numBytes = Math.min(source.remaining(), destination.remaining());
		final ByteBuffer tmp = source.asReadOnlyBuffer();
		tmp.limit(tmp.position() + numBytes);
		destination.put(tmp);
		source.position(tmp.position()); // until now only tmp pos has been incremented, so we need to adjust the position
		return numBytes;
	}

}

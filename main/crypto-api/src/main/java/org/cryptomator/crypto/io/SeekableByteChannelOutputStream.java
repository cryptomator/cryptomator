/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.crypto.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class SeekableByteChannelOutputStream extends OutputStream {

	private final SeekableByteChannel channel;

	public SeekableByteChannelOutputStream(SeekableByteChannel channel) {
		this.channel = channel;
	}

	@Override
	public void write(int b) throws IOException {
		final byte actualByte = (byte) (b & 0x000000FF);
		final ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put(actualByte);
		channel.write(buffer);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		final ByteBuffer buffer = ByteBuffer.wrap(b, off, len);
		channel.write(buffer);
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	/**
	 * @see SeekableByteChannel#truncate(long)
	 */
	public void truncate(long size) throws IOException {
		channel.truncate(size);
	}

	/**
	 * @see SeekableByteChannel#position()
	 */
	public long position() throws IOException {
		return channel.position();
	}

	/**
	 * @see SeekableByteChannel#position(long)
	 */
	public void position(long newPosition) throws IOException {
		channel.position(newPosition);
	}

}

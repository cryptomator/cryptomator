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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class SeekableByteChannelInputStream extends InputStream {
	private final SeekableByteChannel channel;
	private volatile long markedPos = 0;

	public SeekableByteChannelInputStream(SeekableByteChannel channel) {
		this.channel = channel;
	}

	@Override
	public int read() throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocate(1);
		final int read = channel.read(buffer);
		if (read == 1) {
			return buffer.get(0);
		} else {
			return -1;
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		final ByteBuffer buffer = ByteBuffer.wrap(b, off, len);
		return channel.read(buffer);
	}

	@Override
	public int available() throws IOException {
		long available = channel.size() - channel.position();
		if (available > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		} else {
			return (int) available;
		}
	}

	@Override
	public long skip(long n) throws IOException {
		final long pos = channel.position();
		final long max = channel.size();
		final long maxSkip = max - pos;
		final long actualSkip = Math.min(n, maxSkip);
		channel.position(channel.position() + actualSkip);
		return actualSkip;
	}

	@Override
	public void close() throws IOException {
		channel.close();
		super.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		try {
			markedPos = channel.position();
		} catch (IOException e) {
			markedPos = 0;
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		channel.position(markedPos);
	}

	public synchronized void resetTo(long position) throws IOException {
		channel.position(position);
	}

	@Override
	public boolean markSupported() {
		return true;
	}

}
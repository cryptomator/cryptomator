package org.cryptomator.crypto.aes256;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

class ByteBufferBackedSeekableChannel implements SeekableByteChannel {

	private final ByteBuffer buffer;
	private boolean open = true;

	ByteBufferBackedSeekableChannel(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void close() throws IOException {
		open = false;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		if (buffer.remaining() == 0) {
			return -1;
		}
		int num = Math.min(dst.remaining(), buffer.remaining());
		byte[] bytes = new byte[num];
		buffer.get(bytes);
		dst.put(bytes);
		return num;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		int num = src.remaining();
		if (buffer.remaining() < src.remaining()) {
			buffer.limit(buffer.limit() + src.remaining());
		}
		buffer.put(src);
		return num;
	}

	@Override
	public long position() throws IOException {
		return buffer.position();
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		if (newPosition > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException();
		}
		if (newPosition > buffer.limit()) {
			buffer.limit((int) newPosition);
		}
		buffer.position((int) newPosition);
		return this;
	}

	@Override
	public long size() throws IOException {
		return buffer.limit();
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		if (size > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException();
		}
		buffer.limit((int) size);
		return this;
	}

}

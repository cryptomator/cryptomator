package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static org.cryptomator.filesystem.nio.OpenMode.READ;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Path;

import org.cryptomator.filesystem.ReadableFile;

class ReadableNioFile implements ReadableFile {

	private final Path path;
	private final SharedFileChannel channel;
	private final Runnable afterCloseCallback;

	private boolean open = true;
	private long position = 0;

	public ReadableNioFile(Path path, SharedFileChannel channel, Runnable afterCloseCallback) {
		this.path = path;
		this.channel = channel;
		this.afterCloseCallback = afterCloseCallback;
		channel.open(READ);
	}

	@Override
	public int read(ByteBuffer target) throws UncheckedIOException {
		assertOpen();
		int read = channel.readFully(position, target);
		if (read != SharedFileChannel.EOF) {
			position += read;
		}
		return read;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public long size() throws UncheckedIOException {
		return channel.size();
	}

	@Override
	public void position(long position) throws UncheckedIOException {
		assertOpen();
		if (position < 0) {
			throw new IllegalArgumentException();
		}
		this.position = position;
	}

	@Override
	public void close() {
		if (!open) {
			return;
		}
		open = false;
		try {
			channel.close();
		} finally {
			afterCloseCallback.run();
		}
	}

	private void assertOpen() {
		if (!open) {
			throw new UncheckedIOException(format("%s already closed.", this), new ClosedChannelException());
		}
	}

	@Override
	public String toString() {
		return format("ReadableNioFile(%s)", path);
	}

}
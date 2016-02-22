package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static org.cryptomator.filesystem.nio.OpenMode.WRITE;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Path;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.WritableFile;

class WritableNioFile implements WritableFile {

	private final FileSystem fileSystem;
	private final Path path;
	private final SharedFileChannel channel;
	private final Runnable afterCloseCallback;

	private boolean open = true;
	private boolean channelOpened = false;
	private long position = 0;

	public WritableNioFile(FileSystem fileSystem, Path path, SharedFileChannel channel, Runnable afterCloseCallback) {
		this.fileSystem = fileSystem;
		this.path = path;
		this.channel = channel;
		this.afterCloseCallback = afterCloseCallback;
	}

	@Override
	public int write(ByteBuffer source) throws UncheckedIOException {
		assertOpen();
		ensureChannelIsOpened();
		int written = channel.writeFully(position, source);
		position += written;
		return written;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void position(long position) throws UncheckedIOException {
		assertOpen();
		this.position = position;
	}

	@Override
	public void truncate() throws UncheckedIOException {
		assertOpen();
		ensureChannelIsOpened();
		channel.truncate(0);
	}

	@Override
	public void close() throws UncheckedIOException {
		if (!open) {
			return;
		}
		open = false;
		try {
			closeChannelIfOpened();
		} finally {
			afterCloseCallback.run();
		}
	}

	void ensureChannelIsOpened() {
		if (!channelOpened) {
			channel.open(WRITE);
			channelOpened = true;
		}
	}

	void closeChannelIfOpened() {
		if (channelOpened) {
			channel.close();
		}
	}

	FileSystem fileSystem() {
		return fileSystem;
	}

	Path path() {
		return path;
	}

	SharedFileChannel channel() {
		return channel;
	}

	void invokeAfterCloseCallback() {
		afterCloseCallback.run();
	}

	void assertOpen() {
		if (!open) {
			throw new UncheckedIOException(format("%s already closed.", this), new ClosedChannelException());
		}
	}

	@Override
	public String toString() {
		return format("WritableNioFile(%s)", path);
	}

}
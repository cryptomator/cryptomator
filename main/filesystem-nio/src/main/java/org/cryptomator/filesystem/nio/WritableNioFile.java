package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.cryptomator.filesystem.nio.OpenMode.WRITE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.cryptomator.filesystem.FileSystem;
import org.cryptomator.filesystem.WritableFile;

class WritableNioFile implements WritableFile {

	private final FileSystem fileSystem;
	private final Path path;
	private final SharedFileChannel channel;
	private final NioAccess nioAccess;
	private Runnable afterCloseCallback;

	private boolean open = true;
	private long position = 0;

	public WritableNioFile(FileSystem fileSystem, Path path, SharedFileChannel channel, Runnable afterCloseCallback, NioAccess nioAccess) {
		this.fileSystem = fileSystem;
		this.path = path;
		this.channel = channel;
		this.afterCloseCallback = afterCloseCallback;
		this.nioAccess = nioAccess;
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

	private boolean belongsToSameFilesystem(WritableFile other) {
		return other instanceof WritableNioFile && ((WritableNioFile) other).fileSystem() == fileSystem;
	}

	@Override
	public void moveTo(WritableFile other) throws UncheckedIOException {
		assertOpen();
		if (other == this) {
			return;
		} else if (belongsToSameFilesystem(other)) {
			internalMoveTo((WritableNioFile) other);
		} else {
			throw new IllegalArgumentException("Can only move to a WritableFile from the same FileSystem");
		}
	}

	private void internalMoveTo(WritableNioFile other) {
		other.assertOpen();
		assertMovePreconditionsAreMet(other);
		try {
			closeChannelIfOpened();
			other.closeChannelIfOpened();
			nioAccess.move(path, other.path(), REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			open = false;
			other.open = false;
			other.invokeAfterCloseCallback();
			invokeAfterCloseCallback();
		}
	}

	private void assertMovePreconditionsAreMet(WritableNioFile other) {
		if (nioAccess.isDirectory(path)) {
			throw new UncheckedIOException(new IOException(format("Can not move %s to %s. Source is a directory", path, other.path())));
		}
		if (nioAccess.isDirectory(other.path())) {
			throw new UncheckedIOException(new IOException(format("Can not move %s to %s. Target is a directory", path, other.path())));
		}
	}

	@Override
	public void setLastModified(Instant instant) throws UncheckedIOException {
		assertOpen();
		ensureChannelIsOpened();
		try {
			nioAccess.setLastModifiedTime(path, FileTime.from(instant));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void delete() throws UncheckedIOException {
		assertOpen();
		try {
			closeChannelIfOpened();
			nioAccess.delete(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			open = false;
			invokeAfterCloseCallback();
		}
	}

	@Override
	public void truncate() throws UncheckedIOException {
		assertOpen();
		ensureChannelIsOpened();
		channel.truncate(0);
	}

	@Override
	public void setCreationTime(Instant instant) throws UncheckedIOException {
		assertOpen();
		ensureChannelIsOpened();
		try {
			nioAccess.setCreationTime(path, FileTime.from(instant));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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
		channel.openIfClosed(WRITE);
	}

	void closeChannelIfOpened() {
		channel.closeIfOpen();
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
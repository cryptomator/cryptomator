package org.cryptomator.filesystem.nio;

import static java.lang.String.format;
import static org.cryptomator.filesystem.nio.OpenMode.READ;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class ReadableNioFile implements ReadableFile {

	private final NioFile nioFile;

	private boolean open = true;
	private long position = 0;

	public ReadableNioFile(NioFile nioFile) {
		this.nioFile = nioFile;
		nioFile.channel().open(READ);
	}

	@Override
	public int read(ByteBuffer target) throws UncheckedIOException {
		assertOpen();
		int read = nioFile.channel().readFully(position, target);
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
	public void position(long position) throws UncheckedIOException {
		assertOpen();
		if (position < 0) {
			throw new IllegalArgumentException();
		}
		this.position = position;
	}

	@Override
	public void copyTo(WritableFile other) throws UncheckedIOException {
		assertOpen();
		if (belongsToSameFilesystem(other)) {
			internalCopyTo((WritableNioFile) other);
		} else {
			throw new IllegalArgumentException("Can only copy to a WritableFile from the same FileSystem");
		}
	}

	private boolean belongsToSameFilesystem(WritableFile other) {
		return other instanceof WritableNioFile && ((WritableNioFile) other).nioFile().belongsToSameFilesystem(nioFile);
	}

	private void internalCopyTo(WritableNioFile target) {
		target.assertOpen();
		target.ensureChannelIsOpened();
		SharedFileChannel targetChannel = target.channel();
		targetChannel.truncate(0);
		long size = nioFile.channel().size();
		long transferred = 0;
		while (transferred < size) {
			transferred += nioFile.channel().transferTo(transferred, size - transferred, targetChannel, transferred);
		}
	}

	@Override
	public void close() {
		if (!open) {
			return;
		}
		open = false;
		try {
			nioFile.channel().close();
		} finally {
			nioFile.lock().readLock().unlock();
		}
	}

	private void assertOpen() {
		if (!open) {
			throw new UncheckedIOException(format("%s already closed.", this), new ClosedChannelException());
		}
	}

	@Override
	public String toString() {
		return format("Readable%s", nioFile);
	}

}
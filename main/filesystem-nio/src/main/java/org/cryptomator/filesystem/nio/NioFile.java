package org.cryptomator.filesystem.nio;

import static java.lang.String.format;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class NioFile extends NioNode implements File {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

	public NioFile(Optional<NioFolder> parent, Path path) {
		super(parent, path);
	}

	@Override
	public ReadableFile openReadable() throws UncheckedIOException {
		if (lock.getWriteHoldCount() > 0) {
			throw new IllegalStateException("Current thread is currently reading this file");
		}
		lock.readLock().lock();
		return new ReadableView();
	}

	@Override
	public WritableFile openWritable() throws UncheckedIOException {
		if (lock.getReadHoldCount() > 0) {
			throw new IllegalStateException("Current thread is currently reading this file");
		}
		lock.readLock().lock();
		return new WritableView();
	}

	private class ReadableView implements ReadableFile {

		@Override
		public void read(ByteBuffer target) throws UncheckedIOException {
		}

		@Override
		public void read(ByteBuffer target, long position) throws UncheckedIOException {
		}

		@Override
		public void copyTo(WritableFile other) throws UncheckedIOException {
		}

		@Override
		public void close() throws UncheckedIOException {
		}

	}

	private class WritableView implements WritableFile {

		@Override
		public void write(ByteBuffer source) throws UncheckedIOException {
		}

		@Override
		public void write(ByteBuffer source, int position) throws UncheckedIOException {
		}

		@Override
		public void moveTo(WritableFile other) throws UncheckedIOException {
		}

		@Override
		public void setLastModified(Instant instant) throws UncheckedIOException {
		}

		@Override
		public void delete() throws UncheckedIOException {
		}

		@Override
		public void truncate() throws UncheckedIOException {
		}

		@Override
		public void close() throws UncheckedIOException {
		}

	}

	@Override
	public int compareTo(File o) {
		if (belongsToSameFilesystem(o)) {
			assert o instanceof NioNode;
			return path.compareTo(((NioFile) o).path);
		} else {
			throw new IllegalArgumentException("Can not mix File objects from different file systems");
		}
	}

	@Override
	public String toString() {
		return format("NioFile(%s)", path);
	}

}

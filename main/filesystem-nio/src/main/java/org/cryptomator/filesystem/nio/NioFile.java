package org.cryptomator.filesystem.nio;

import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class NioFile extends NioNode implements File {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	private SharedFileChannel sharedChannel;

	public NioFile(Optional<NioFolder> parent, Path eventuallyNonAbsolutePath, NioAccess nioAccess, InstanceFactory instanceFactory) {
		super(parent, eventuallyNonAbsolutePath, nioAccess, instanceFactory);
		sharedChannel = instanceFactory.sharedFileChannel(path, nioAccess);
	}

	@Override
	public ReadableFile openReadable() throws UncheckedIOException {
		if (lock.getWriteHoldCount() > 0) {
			throw new IllegalStateException("Current thread is currently writing this file");
		}
		if (lock.getReadHoldCount() > 0) {
			throw new IllegalStateException("Current thread is already reading this file");
		}
		lock.readLock().lock();
		return instanceFactory.readableNioFile(fileSystem(), path, sharedChannel, this::unlockReadLock);
	}

	private void unlockReadLock() {
		lock.readLock().unlock();
	}

	@Override
	public WritableFile openWritable() throws UncheckedIOException {
		if (lock.getWriteHoldCount() > 0) {
			throw new IllegalStateException("Current thread is already writing this file");
		}
		if (lock.getReadHoldCount() > 0) {
			throw new IllegalStateException("Current thread is currently reading this file");
		}
		lock.writeLock().lock();
		return instanceFactory.writableNioFile(fileSystem(), path, sharedChannel, this::unlockWriteLock, nioAccess);
	}

	private void unlockWriteLock() {
		lock.writeLock().unlock();
	}

	@Override
	public boolean exists() throws UncheckedIOException {
		return nioAccess.isRegularFile(path);
	}

	@Override
	public Instant lastModified() throws UncheckedIOException {
		if (nioAccess.exists(path) && !exists()) {
			throw new UncheckedIOException(new IOException(format("%s is a folder", path)));
		}
		return super.lastModified();
	}

	@Override
	public int compareTo(File o) {
		if (belongsToSameFilesystem(o)) {
			return path.compareTo(((NioFile) o).path);
		} else {
			throw new IllegalArgumentException("Can not mix File objects from different file systems");
		}
	}

	@Override
	public String toString() {
		return format("NioFile(%s)", path);
	}

	@Override
	public Optional<Instant> creationTime() throws UncheckedIOException {
		if (nioAccess.exists(path) && !exists()) {
			throw new UncheckedIOException(new IOException(format("%s is a folder", path)));
		}
		return super.creationTime();
	}

}

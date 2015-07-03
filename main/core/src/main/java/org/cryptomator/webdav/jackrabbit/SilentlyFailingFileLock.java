package org.cryptomator.webdav.jackrabbit;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this class wrap a file lock, that is created upon construction and destroyed by {@link #close()}.
 * 
 * If the construction fails (e.g. if the file system does not support locks) no exception will be thrown and no lock is created.
 */
class SilentlyFailingFileLock implements AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(SilentlyFailingFileLock.class);

	private final FileLock lock;

	/**
	 * Invokes #SilentlyFailingFileLock(FileChannel, long, long, boolean) with a position of 0 and a size of {@link Long#MAX_VALUE}.
	 */
	SilentlyFailingFileLock(FileChannel channel, boolean shared) {
		this(channel, 0L, Long.MAX_VALUE, shared);
	}

	/**
	 * @throws NonReadableChannelException If shared is true this channel was not opened for reading
	 * @throws NonWritableChannelException If shared is false but this channel was not opened for writing
	 * @see FileChannel#lock(long, long, boolean)
	 */
	SilentlyFailingFileLock(FileChannel channel, long position, long size, boolean shared) {
		FileLock lock = null;
		try {
			lock = channel.tryLock(position, size, shared);
		} catch (IOException | OverlappingFileLockException e) {
			if (LOG.isDebugEnabled()) {
				LOG.warn("Unable to lock file.");
			}
		} finally {
			this.lock = lock;
		}
	}

	@Override
	public void close() throws IOException {
		if (lock != null) {
			lock.close();
		}
	}

}

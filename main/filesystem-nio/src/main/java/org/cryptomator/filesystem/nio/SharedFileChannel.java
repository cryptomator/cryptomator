package org.cryptomator.filesystem.nio;

import static java.lang.String.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SharedFileChannel {

	public static final int EOF = -1;

	private final Path path;
	private final NioAccess nioAccess;

	private Map<Thread, Thread> openedBy = new ConcurrentHashMap<>();
	private Lock lock = new ReentrantLock();

	private FileChannel delegate;

	public SharedFileChannel(Path path, NioAccess nioAccess) {
		this.path = path;
		this.nioAccess = nioAccess;
	}

	public void openIfClosed(OpenMode mode) {
		if (!openedBy.containsKey(Thread.currentThread())) {
			open(mode);
		}
	}

	public void open(OpenMode mode) {
		doLocked(() -> {
			Thread thread = Thread.currentThread();
			boolean failed = true;
			try {
				if (openedBy.put(thread, thread) != null) {
					throw new IllegalStateException("SharedFileChannel already open for current thread");
				}
				if (delegate == null) {
					createChannel(mode);
				}
				failed = false;
			} finally {
				if (failed) {
					openedBy.remove(thread);
				}
			}
		});
	}

	public void closeIfOpen() {
		if (openedBy.containsKey(Thread.currentThread())) {
			internalClose();
		}
	}

	public void close() {
		assertOpenedByCurrentThread();
		internalClose();
	}

	private void internalClose() {
		doLocked(() -> {
			openedBy.remove(Thread.currentThread());
			try {
				delegate.force(true);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} finally {
				if (openedBy.isEmpty()) {
					closeChannel();
				}
			}
		});
	}

	private void assertOpenedByCurrentThread() {
		if (!openedBy.containsKey(Thread.currentThread())) {
			throw new IllegalStateException("SharedFileChannel closed for current thread");
		}
	}

	private void createChannel(OpenMode mode) {
		try {
			if (nioAccess.isDirectory(path)) {
				throw new IOException(format("%s is a directory", path));
			}
			if (mode == OpenMode.READ) {
				if (!nioAccess.isRegularFile(path)) {
					throw new NoSuchFileException(format("%s does not exist", path));
				}
			}
			delegate = nioAccess.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void closeChannel() {
		try {
			nioAccess.close(delegate);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} finally {
			delegate = null;
		}
	}

	public int readFully(long position, ByteBuffer target) {
		assertOpenedByCurrentThread();
		try {
			return tryReadFully(position, target);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private int tryReadFully(long position, ByteBuffer target) throws IOException {
		int initialRemaining = target.remaining();
		long maxPosition = position + initialRemaining;
		do {
			if (delegate.read(target, maxPosition - target.remaining()) == EOF) {
				if (initialRemaining == target.remaining()) {
					return EOF;
				} else {
					return initialRemaining - target.remaining();
				}
			}
		} while (target.hasRemaining());
		return initialRemaining - target.remaining();
	}

	public void truncate(int i) {
		assertOpenedByCurrentThread();
		try {
			delegate.truncate(i);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public long size() {
		assertOpenedByCurrentThread();
		try {
			return delegate.size();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public long transferTo(long position, long count, SharedFileChannel targetChannel, long targetPosition) {
		assertOpenedByCurrentThread();
		targetChannel.assertOpenedByCurrentThread();
		if (count < 0) {
			throw new IllegalArgumentException("Count must not be negative");
		}
		try {
			long maxPosition = Math.min(delegate.size(), position + count);
			long transferCount = Math.max(0, maxPosition - position);
			long remaining = transferCount;
			targetChannel.delegate.position(targetPosition);
			while (remaining > 0) {
				remaining -= delegate.transferTo(maxPosition - remaining, remaining, targetChannel.delegate);
			}
			return transferCount;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void doLocked(Runnable task) {
		lock.lock();
		try {
			task.run();
		} finally {
			lock.unlock();
		}
	}

	public int writeFully(long position, ByteBuffer source) {
		assertOpenedByCurrentThread();
		try {
			return tryWriteFully(position, source);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private int tryWriteFully(long position, ByteBuffer source) throws IOException {
		int count = source.remaining();
		long maxPosition = position + count;
		do {
			delegate.write(source, maxPosition - source.remaining());
		} while (source.hasRemaining());
		return count;
	}

}

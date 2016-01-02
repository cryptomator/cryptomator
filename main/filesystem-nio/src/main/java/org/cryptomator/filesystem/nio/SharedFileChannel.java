package org.cryptomator.filesystem.nio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SharedFileChannel {

	public static final int EOF = -1;

	private final Path path;

	private Map<Thread, Thread> openedBy = new ConcurrentHashMap<>();
	private Lock lock = new ReentrantLock();

	private FileChannel delegate;

	public SharedFileChannel(Path path) {
		this.path = path;
	}

	public void open(OpenMode mode) {
		doLocked(() -> {
			Thread thread = Thread.currentThread();
			if (openedBy.put(thread, thread) != null) {
				throw new IllegalStateException("A thread can only open a SharedFileChannel once");
			}
			if (delegate == null) {
				createChannel(mode);
			}
		});
	}

	public void close() {
		assertOpenedByCurrentThread();
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

	/**
	 * @deprecated only intended to be used in tests
	 */
	@Deprecated
	void forceClose() {
		closeSilently(delegate);
	}

	private void assertOpenedByCurrentThread() {
		if (!openedBy.containsKey(Thread.currentThread())) {
			throw new IllegalStateException("SharedFileChannel closed for current thread");
		}
	}

	private void createChannel(OpenMode mode) {
		try {
			FileChannel readChannel = null;
			if (mode == OpenMode.READ) {
				readChannel = FileChannel.open(path, StandardOpenOption.READ);
			}
			delegate = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
			closeSilently(readChannel);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void closeSilently(FileChannel channel) {
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	private void closeChannel() {
		try {
			delegate.close();
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
		try {
			long maxPosition = delegate.size();
			long maxCount = Math.min(count, maxPosition - position);
			long remaining = maxCount;
			targetChannel.delegate.position(targetPosition);
			while (remaining > 0) {
				remaining -= delegate.transferTo(maxPosition - remaining, remaining, targetChannel.delegate);
			}
			return maxCount;
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
		int initialRemaining = source.remaining();
		long maxPosition = position + initialRemaining;
		do {
			delegate.write(source, maxPosition - source.remaining());
		} while (source.hasRemaining());
		return initialRemaining - source.remaining();
	}

}

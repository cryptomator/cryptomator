package org.cryptomator.filesystem.nio;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SharedFileChannel {

	public static final int EOF = -1;

	private final Path path;
	private final NioAccess nioAccess;
	private final OpenCloseCounter openCloseCounter;

	private Lock lock = new ReentrantLock();

	private AsynchronousFileChannel delegate;

	public SharedFileChannel(Path path, NioAccess nioAccess) {
		this.path = path;
		this.nioAccess = nioAccess;
		this.openCloseCounter = new OpenCloseCounter();
	}

	public void open(OpenMode mode) {
		doLocked(() -> {
			boolean failed = true;
			try {
				openCloseCounter.countOpen();
				if (delegate == null) {
					createChannel(mode);
				}
				failed = false;
			} finally {
				if (failed) {
					openCloseCounter.countClose();
				}
			}
		});
	}

	public void close() {
		doLocked(() -> {
			openCloseCounter.countClose();
			try {
				delegate.force(true);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} finally {
				if (!openCloseCounter.isOpen()) {
					closeChannel();
				}
			}
		});
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
		assertOpen();
		try {
			return tryReadFully(position, target);
		} catch (InterruptedException e) {
			throw new UncheckedIOException(new InterruptedIOException("read has been interrupted"));
		} catch (ExecutionException e) {
			throw new UncheckedIOException(new IOException(e));
		}
	}

	private int tryReadFully(long position, ByteBuffer target) throws InterruptedException, ExecutionException {
		int initialRemaining = target.remaining();
		long maxPosition = position + initialRemaining;
		do {
			if (delegate.read(target, maxPosition - target.remaining()).get() == EOF) {
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
		assertOpen();
		try {
			delegate.truncate(i);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public long size() {
		assertOpen();
		try {
			return delegate.size();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public long transferTo(long position, long count, SharedFileChannel targetChannel, long targetPosition) {
		assertOpen();
		targetChannel.assertOpen();
		if (count < 0) {
			throw new IllegalArgumentException("Count must not be negative");
		}
		try {
			ByteBuffer buffer = ByteBuffer.allocate(32 * 1024);
			long maxPosition = Math.min(delegate.size(), position + count);
			long transferCount = Math.max(0, maxPosition - position);
			long transferred = 0;
			while (transferred < transferCount) {
				int read = delegate.read(buffer, position + transferred).get();
				if (read == -1) {
					throw new IllegalStateException("Reached end of file during transfer to");
				}
				buffer.flip();
				while (buffer.hasRemaining()) {
					transferred += targetChannel.delegate.write(buffer, targetPosition + transferred).get();
				}
			}
			return transferCount;
		} catch (InterruptedException e) {
			throw new UncheckedIOException(new InterruptedIOException("read has been interrupted"));
		} catch (ExecutionException e) {
			throw new UncheckedIOException(new IOException(e));
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
		assertOpen();
		try {
			return tryWriteFully(position, source);
		} catch (InterruptedException e) {
			throw new UncheckedIOException(new InterruptedIOException("read has been interrupted"));
		} catch (ExecutionException e) {
			throw new UncheckedIOException(new IOException(e));
		}
	}

	private int tryWriteFully(long position, ByteBuffer source) throws InterruptedException, ExecutionException {
		int count = source.remaining();
		long maxPosition = position + count;
		do {
			delegate.write(source, maxPosition - source.remaining()).get();
		} while (source.hasRemaining());
		return count;
	}

	private void assertOpen() {
		if (!openCloseCounter.isOpen()) {
			throw new IllegalStateException("SharedFileChannel is not open");
		}
	}

}

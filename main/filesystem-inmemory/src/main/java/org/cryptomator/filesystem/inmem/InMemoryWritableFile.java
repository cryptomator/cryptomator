/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.inmem;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.io.ByteBuffers;

public class InMemoryWritableFile implements WritableFile {

	private final Supplier<ByteBuffer> contentGetter;
	private final Consumer<ByteBuffer> contentSetter;
	private final WriteLock writeLock;
	private final AtomicInteger position = new AtomicInteger();
	private final AtomicBoolean open = new AtomicBoolean(true);
	private final ReentrantLock writingLock = new ReentrantLock();

	public InMemoryWritableFile(Supplier<ByteBuffer> contentGetter, Consumer<ByteBuffer> contentSetter, WriteLock writeLock) {
		this.contentGetter = contentGetter;
		this.contentSetter = contentSetter;
		this.writeLock = writeLock;
	}

	@Override
	public boolean isOpen() {
		return open.get();
	}

	@Override
	public void truncate() throws UncheckedIOException {
		writingLock.lock();
		try {
			contentSetter.accept(InMemoryFile.createNewEmptyByteBuffer());
			position.set(0);
		} finally {
			writingLock.unlock();
		}
	}

	@Override
	public int write(ByteBuffer source) throws UncheckedIOException {
		writingLock.lock();
		try {
			ByteBuffer content = contentGetter.get();
			int prevLimit = content.limit();

			int toBeCopied = source.remaining();
			int pos = position.getAndAdd(toBeCopied);
			int ourLimit = pos + toBeCopied;
			int newLimit = Math.max(prevLimit, ourLimit);

			ByteBuffer destination = ensureCapacity(content, newLimit);
			destination.limit(newLimit).position(pos);
			return ByteBuffers.copy(source, destination);
		} finally {
			writingLock.unlock();
		}
	}

	private ByteBuffer ensureCapacity(ByteBuffer buf, int limit) {
		assert writingLock.isHeldByCurrentThread();
		if (buf.capacity() < limit) {
			int oldPos = buf.position();
			buf.clear();
			int newBufferSize = Math.max(limit, (int) (buf.capacity() * InMemoryFile.GROWTH_RATE));
			ByteBuffer newBuf = ByteBuffer.allocate(newBufferSize);
			ByteBuffers.copy(buf, newBuf);
			newBuf.limit(limit).position(oldPos);
			contentSetter.accept(newBuf);
			return newBuf;
		} else {
			return buf;
		}
	}

	@Override
	public void position(long position) throws UncheckedIOException {
		assert position < Integer.MAX_VALUE : "Can not use that big in-memory files.";
		this.position.set((int) position);
	}

	@Override
	public void close() throws UncheckedIOException {
		open.set(false);
		writeLock.unlock();
	}

}

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
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.function.Supplier;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.io.ByteBuffers;

class InMemoryReadableFile implements ReadableFile {

	private final Supplier<ByteBuffer> contentGetter;
	private final ReadLock readLock;
	private final AtomicInteger position = new AtomicInteger();
	private final AtomicBoolean open = new AtomicBoolean(true);

	public InMemoryReadableFile(Supplier<ByteBuffer> contentGetter, ReadLock readLock) {
		this.contentGetter = contentGetter;
		this.readLock = readLock;
	}

	@Override
	public boolean isOpen() {
		return open.get();
	}

	@Override
	public int read(ByteBuffer destination) throws UncheckedIOException {
		ByteBuffer source = contentGetter.get().asReadOnlyBuffer();
		int toBeCopied = destination.remaining();
		int pos = position.getAndAdd(toBeCopied);
		if (pos >= source.limit()) {
			return -1;
		} else {
			source.position(pos);
			assert source.hasRemaining();
			int numRead = ByteBuffers.copy(source, destination);
			assert numRead <= toBeCopied;
			return numRead;
		}
	}

	@Override
	public long size() throws UncheckedIOException {
		return contentGetter.get().limit();
	}

	@Override
	public void position(long position) throws UncheckedIOException {
		assert position < Integer.MAX_VALUE : "Can not use that big in-memory files.";
		this.position.set((int) position);
	}

	@Override
	public void close() throws UncheckedIOException {
		open.set(false);
		readLock.unlock();
	}

}

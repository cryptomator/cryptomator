/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.inmem;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.function.Supplier;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.io.ByteBuffers;

class InMemoryReadableFile implements ReadableFile {

	private final Supplier<ByteBuffer> contentGetter;
	private final ReadLock readLock;
	private boolean open = true;
	private volatile int position = 0;

	public InMemoryReadableFile(Supplier<ByteBuffer> contentGetter, ReadLock readLock) {
		this.contentGetter = contentGetter;
		this.readLock = readLock;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void copyTo(WritableFile other) throws UncheckedIOException {
		ByteBuffer source = contentGetter.get().asReadOnlyBuffer();
		source.position(0);
		other.truncate();
		this.position += other.write(source);
	}

	@Override
	public int read(ByteBuffer destination) throws UncheckedIOException {
		ByteBuffer source = contentGetter.get().asReadOnlyBuffer();
		if (position >= source.limit()) {
			return -1;
		} else {
			source.position(position);
			assert source.hasRemaining();
			int numRead = ByteBuffers.copy(source, destination);
			this.position += numRead;
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
		this.position = (int) position;
	}

	@Override
	public void close() throws UncheckedIOException {
		open = false;
		readLock.unlock();
	}

}

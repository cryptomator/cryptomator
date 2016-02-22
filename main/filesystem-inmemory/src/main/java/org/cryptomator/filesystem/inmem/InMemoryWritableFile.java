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
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.io.ByteBuffers;

public class InMemoryWritableFile implements WritableFile {

	private final Supplier<ByteBuffer> contentGetter;
	private final Consumer<ByteBuffer> contentSetter;
	private final WriteLock writeLock;

	private boolean open = true;
	private volatile int position = 0;

	public InMemoryWritableFile(Supplier<ByteBuffer> contentGetter, Consumer<ByteBuffer> contentSetter, WriteLock writeLock) {
		this.contentGetter = contentGetter;
		this.contentSetter = contentSetter;
		this.writeLock = writeLock;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void truncate() throws UncheckedIOException {
		contentSetter.accept(ByteBuffer.allocate(0));
	}

	@Override
	public int write(ByteBuffer source) throws UncheckedIOException {
		ByteBuffer destination = contentGetter.get();
		int oldFileSize = destination.limit();
		int requiredSize = position + source.remaining();
		int newFileSize = Math.max(oldFileSize, requiredSize);
		if (destination.capacity() < requiredSize) {
			ByteBuffer old = destination;
			old.clear();
			int newBufferSize = Math.max(requiredSize, (int) (destination.capacity() * InMemoryFile.GROWTH_RATE));
			destination = ByteBuffer.allocate(newBufferSize);
			ByteBuffers.copy(old, destination);
			contentSetter.accept(destination);
		}
		destination.limit(newFileSize);
		destination.position(position);
		int numWritten = ByteBuffers.copy(source, destination);
		this.position += numWritten;
		return numWritten;
	}

	@Override
	public void position(long position) throws UncheckedIOException {
		assert position < Integer.MAX_VALUE : "Can not use that big in-memory files.";
		this.position = (int) position;
	}

	@Override
	public void close() throws UncheckedIOException {
		open = false;
		writeLock.unlock();
	}

}

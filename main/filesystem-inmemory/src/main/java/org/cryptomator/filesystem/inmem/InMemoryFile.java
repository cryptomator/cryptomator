/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.inmem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class InMemoryFile extends InMemoryNode implements ReadableFile, WritableFile {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private ByteBuffer content = ByteBuffer.wrap(new byte[0]);

	public InMemoryFile(InMemoryFolder parent, String name, Instant lastModified) {
		super(parent, name, lastModified);
	}

	@Override
	public ReadableFile openReadable(long timeout, TimeUnit unit) throws IOException, TimeoutException {
		if (!exists()) {
			throw new FileNotFoundException(this.name() + " does not exist");
		}
		try {
			if (!lock.readLock().tryLock(timeout, unit)) {
				throw new TimeoutException("Failed to open " + name() + " for reading within time limit.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return this;
	}

	@Override
	public WritableFile openWritable(long timeout, TimeUnit unit) throws IOException, TimeoutException {
		try {
			if (!lock.writeLock().tryLock(timeout, unit)) {
				throw new TimeoutException("Failed to open " + name() + " for writing within time limit.");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		final InMemoryFolder parent = parent().get();
		try {
			parent.children.compute(this.name(), (k, v) -> {
				if (v != null && v != this) {
					throw new IllegalStateException("More than one representation of same file");
				}
				return this;
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
		return this;
	}

	@Override
	public void read(ByteBuffer target) throws IOException {
		this.read(target, 0);
	}

	@Override
	public void read(ByteBuffer target, int position) throws IOException {
		content.rewind();
		content.position(position);
		target.put(content);
	}

	@Override
	public void write(ByteBuffer source) throws IOException {
		this.write(source, content.position());
	}

	@Override
	public void write(ByteBuffer source, int position) throws IOException {
		assert content != null;
		if (position + source.remaining() > content.remaining()) {
			// create bigger buffer
			ByteBuffer tmp = ByteBuffer.allocate(Math.max(position, content.capacity()) + source.remaining());
			tmp.put(content);
			content = tmp;
		}
		content.position(position);
		content.put(source);
	}

	@Override
	public WritableFile moveTo(WritableFile other) throws IOException {
		this.copyTo(other);
		this.delete();
		return other;
	}

	@Override
	public void setLastModified(Instant instant) {
		this.lastModified = instant;
	}

	@Override
	public void delete() {
		final InMemoryFolder parent = parent().get();
		parent.children.computeIfPresent(this.name(), (k, v) -> {
			truncate();
			// returning null removes the entry.
			return null;
		});
	}

	@Override
	public void truncate() {
		content = ByteBuffer.wrap(new byte[0]);
	}

	@Override
	public WritableFile copyTo(WritableFile other) throws IOException {
		content.rewind();
		other.truncate();
		other.write(content);
		return other;
	}

	@Override
	public void close() throws IOException {
		if (lock.isWriteLockedByCurrentThread()) {
			lock.writeLock().unlock();
		} else if (lock.getReadHoldCount() > 0) {
			lock.readLock().unlock();
		}
	}

	@Override
	public String toString() {
		return parent.toString() + name;
	}

}

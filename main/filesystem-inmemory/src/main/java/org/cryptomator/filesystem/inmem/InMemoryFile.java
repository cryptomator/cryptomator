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
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;
import org.cryptomator.io.ByteBuffers;

class InMemoryFile extends InMemoryNode implements File, ReadableFile, WritableFile {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private ByteBuffer content = ByteBuffer.wrap(new byte[0]);

	public InMemoryFile(InMemoryFolder parent, String name, Instant lastModified) {
		super(parent, name, lastModified);
	}

	@Override
	public ReadableFile openReadable() {
		if (!exists()) {
			throw new UncheckedIOException(new FileNotFoundException(this.name() + " does not exist"));
		}
		lock.readLock().lock();
		content.rewind();
		return this;
	}

	@Override
	public WritableFile openWritable() {
		lock.writeLock().lock();
		final InMemoryFolder parent = parent().get();
		parent.children.compute(this.name(), (k, v) -> {
			if (v != null && v != this) {
				throw new IllegalStateException("More than one representation of same file");
			}
			return this;
		});
		return this;
	}

	@Override
	public void read(ByteBuffer target) {
		ByteBuffers.copy(content, target);
	}

	@Override
	public void read(ByteBuffer target, int position) {
		content.position(position);
		ByteBuffers.copy(content, target);
	}

	@Override
	public void write(ByteBuffer source) {
		this.write(source, content.position());
	}

	@Override
	public void write(ByteBuffer source, int position) {
		assert content != null;
		expandContentCapacityIfRequired(position + source.remaining());
		content.position(position);
		assert content.remaining() >= source.remaining();
		content.put(source);
	}

	private void expandContentCapacityIfRequired(int requiredCapacity) {
		if (requiredCapacity > content.capacity()) {
			final int currentPos = content.position();
			final ByteBuffer tmp = ByteBuffer.allocate(requiredCapacity);
			content.rewind();
			ByteBuffers.copy(content, tmp);
			content = tmp;
			content.position(currentPos);
		}
	}

	@Override
	public void setLastModified(Instant instant) {
		this.lastModified = instant;
	}

	@Override
	public void truncate() {
		content = ByteBuffer.wrap(new byte[0]);
	}

	@Override
	public void copyTo(WritableFile other) {
		content.rewind();
		other.truncate();
		other.write(content);
	}

	@Override
	public void moveTo(WritableFile other) {
		this.copyTo(other);
		this.delete();
	}

	@Override
	public void delete() {
		final InMemoryFolder parent = parent().get();
		parent.children.computeIfPresent(this.name(), (k, v) -> {
			truncate();
			// returning null removes the entry.
			return null;
		});
		assert!this.exists();
	}

	@Override
	public void close() {
		if (lock.isWriteLockedByCurrentThread()) {
			this.setLastModified(Instant.now());
			lock.writeLock().unlock();
		} else if (lock.getReadHoldCount() > 0) {
			lock.readLock().unlock();
		}
	}

	@Override
	public String toString() {
		return parent.toString() + name;
	}

	@Override
	public int compareTo(File o) {
		return toString().compareTo(o.toString());
	}

}

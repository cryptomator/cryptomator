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
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

class InMemoryFile extends InMemoryNode implements File {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private ByteBuffer content = ByteBuffer.allocate(0);

	public InMemoryFile(InMemoryFolder parent, String name, Instant lastModified) {
		super(parent, name, lastModified);
	}

	@Override
	public ReadableFile openReadable() {
		if (!exists()) {
			throw new UncheckedIOException(new FileNotFoundException(this.name() + " does not exist"));
		}
		final ReadLock readLock = lock.readLock();
		readLock.lock();
		return new InMemoryReadableFile(this::getContent, readLock);
	}

	@Override
	public WritableFile openWritable() {
		final WriteLock writeLock = lock.writeLock();
		writeLock.lock();
		final InMemoryFolder parent = parent().get();
		parent.children.compute(this.name(), (k, v) -> {
			if (v != null && v != this) {
				throw new IllegalStateException("More than one representation of same file");
			}
			return this;
		});
		return new InMemoryWritableFile(this::setLastModified, this::getContent, this::setContent, this::delete, writeLock);
	}

	private void setLastModified(Instant lastModified) {
		this.lastModified = lastModified;
	}

	private ByteBuffer getContent() {
		return content;
	}

	private void setContent(ByteBuffer content) {
		this.content = content;
	}

	private void delete(Void param) {
		final InMemoryFolder parent = parent().get();
		parent.children.computeIfPresent(this.name(), (k, v) -> {
			// returning null removes the entry.
			return null;
		});
		assert!this.exists();
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

/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.inmem;

import static java.lang.String.format;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.cryptomator.common.WeakValuedCache;
import org.cryptomator.filesystem.Folder;

class InMemoryFolder extends InMemoryNode implements Folder {

	final Map<String, InMemoryNode> existingChildren = new ConcurrentHashMap<>();

	private final WeakValuedCache<String, InMemoryFolder> folders = WeakValuedCache.usingLoader(this::newFolder);
	private final WeakValuedCache<String, InMemoryFile> files = WeakValuedCache.usingLoader(this::newFile);

	public InMemoryFolder(InMemoryFolder parent, String name, Instant lastModified, Instant creationTime) {
		super(parent, name, lastModified, creationTime);
	}

	@Override
	public Stream<InMemoryNode> children() {
		if (exists()) {
			return existingChildren.values().stream();
		} else {
			throw new UncheckedIOException(new FileNotFoundException(format("Folder %s does not exist", this)));
		}
	}

	@Override
	public InMemoryFile file(String name) {
		return files.get(name);
	}

	private InMemoryFile newFile(String name) {
		return new InMemoryFile(this, name, Instant.MIN, Instant.MIN);
	}

	@Override
	public InMemoryFolder folder(String name) {
		return folders.get(name);
	}

	private InMemoryFolder newFolder(String name) {
		return new InMemoryFolder(this, name, Instant.MIN, Instant.MIN);
	}

	@Override
	public void create() {
		if (exists()) {
			return;
		}
		parent.create();
		parent.existingChildren.compute(name, (k, v) -> {
			if (v != null) {
				// other file or folder with same name already exists.
				throw new UncheckedIOException(new FileAlreadyExistsException(k));
			} else {
				this.lastModified = Instant.now();
				return this;
			}
		});
		assert this.exists();
		creationTime = Instant.now();
	}

	@Override
	public void moveTo(Folder target) {
		if (target.exists()) {
			target.delete();
		}
		assert!target.exists();
		target.create();
		this.copyTo(target);
		this.delete();
		assert!this.exists();
	}

	@Override
	public void delete() {
		// remove ourself from parent:
		parent.existingChildren.computeIfPresent(name, (k, v) -> {
			// returning null removes the entry.
			return null;
		});
		// delete all children:
		for (Iterator<Map.Entry<String, InMemoryNode>> iterator = existingChildren.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, InMemoryNode> entry = iterator.next();
			iterator.remove();
			// recursively on folders:
			if (entry.getValue() instanceof InMemoryFolder) {
				InMemoryFolder subFolder = (InMemoryFolder) entry.getValue();
				// this will try to itself from our children, which is ok as
				// we're using an iterator here.
				subFolder.delete();
			}
		}
		assert!this.exists();
	}

	@Override
	public String toString() {
		return parent.toString() + name + "/";
	}

}

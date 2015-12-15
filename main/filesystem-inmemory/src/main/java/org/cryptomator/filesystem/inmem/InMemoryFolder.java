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
import java.nio.file.FileAlreadyExistsException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.commons.io.FileExistsException;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.FolderCreateMode;

class InMemoryFolder extends InMemoryNode implements Folder {

	final Map<String, InMemoryNode> children = new TreeMap<>();
	final Map<String, InMemoryNode> volatileChildren = new HashMap<>();

	public InMemoryFolder(InMemoryFolder parent, String name, Instant lastModified) {
		super(parent, name, lastModified);
	}

	@Override
	public Stream<InMemoryNode> children() {
		return children.values().stream();
	}

	@Override
	public InMemoryFile file(String name) {
		InMemoryNode node = children.get(name);
		if (node == null) {
			node = volatileChildren.computeIfAbsent(name, (k) -> {
				return new InMemoryFile(this, name, Instant.MIN);
			});
		}
		if (node instanceof InMemoryFile) {
			return (InMemoryFile) node;
		} else {
			throw new UncheckedIOException(new FileAlreadyExistsException(name + " exists, but is not a file."));
		}
	}

	@Override
	public InMemoryFolder folder(String name) {
		InMemoryNode node = children.get(name);
		if (node == null) {
			node = volatileChildren.computeIfAbsent(name, (k) -> {
				return new InMemoryFolder(this, name, Instant.MIN);
			});
		}
		if (node instanceof InMemoryFolder) {
			return (InMemoryFolder) node;
		} else {
			throw new UncheckedIOException(new FileAlreadyExistsException(name + " exists, but is not a folder."));
		}
	}

	@Override
	public void create(FolderCreateMode mode) {
		if (exists()) {
			return;
		}
		if (!parent.exists() && FolderCreateMode.FAIL_IF_PARENT_IS_MISSING.equals(mode)) {
			throw new UncheckedIOException(new FileNotFoundException(parent.name));
		} else if (!parent.exists() && FolderCreateMode.INCLUDING_PARENTS.equals(mode)) {
			parent.create(mode);
		}
		assert parent.exists();
		parent.children.compute(this.name(), (k, v) -> {
			if (v == null) {
				this.lastModified = Instant.now();
				return this;
			} else {
				throw new UncheckedIOException(new FileExistsException(k));
			}
		});
		assert this.exists();
	}

	@Override
	public void moveTo(Folder target) {
		if (target.exists()) {
			target.delete();
		}
		assert!target.exists();
		target.create(FolderCreateMode.INCLUDING_PARENTS);
		this.copyTo(target);
		this.delete();
		assert!this.exists();
	}

	@Override
	public void delete() {
		// delete subfolder recursively:
		folders().forEach(Folder::delete);
		// delete direct children (this deletes files):
		this.children.clear();
		// remove ourself from parent:
		parent.children.computeIfPresent(name, (k, v) -> {
			// returning null removes the entry.
			return null;
		});
		assert!this.exists();
	}

	@Override
	public String toString() {
		return parent.toString() + name + "/";
	}

}

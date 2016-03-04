/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
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
import java.time.Instant;
import java.util.Optional;

import org.cryptomator.filesystem.Node;

abstract class InMemoryNode implements Node {

	protected final InMemoryFolder parent;
	protected final String name;
	protected Instant lastModified;
	protected Instant creationTime;

	public InMemoryNode(InMemoryFolder parent, String name, Instant lastModified, Instant creationTime) {
		this.parent = parent;
		this.name = name;
		this.lastModified = lastModified;
		this.creationTime = creationTime;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public Optional<InMemoryFolder> parent() {
		return Optional.of(parent);
	}

	@Override
	public boolean exists() {
		return parent.exists() && parent.children().anyMatch(node -> node.equals(this));
	}

	@Override
	public Instant lastModified() {
		if (!exists()) {
			throw new UncheckedIOException(new FileNotFoundException("File does not exist"));
		}
		return lastModified;
	}

	@Override
	public void setLastModified(Instant lastModified) throws UncheckedIOException {
		this.lastModified = lastModified;
	}

	@Override
	public Optional<Instant> creationTime() throws UncheckedIOException {
		if (exists()) {
			return Optional.of(creationTime);
		} else {
			throw new UncheckedIOException(new IOException("Node does not exist"));
		}
	}

	@Override
	public void setCreationTime(Instant creationTime) throws UncheckedIOException {
		this.creationTime = creationTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof InMemoryNode) {
			InMemoryNode other = (InMemoryNode) obj;
			return this.getClass() == other.getClass() //
					&& (this.parent == null && other.parent == null || this.parent != null && this.parent.equals(other.parent)) //
					&& (this.name == null && other.name == null || this.name != null && this.name.equals(other.name));
		} else {
			return false;
		}
	}

}

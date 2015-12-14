package org.cryptomator.filesystem.inmem;

import java.time.Instant;
import java.util.Optional;

import org.cryptomator.filesystem.Node;

public class InMemoryNode implements Node {

	protected final InMemoryFolder parent;
	protected final String name;
	protected Instant lastModified;

	InMemoryNode(InMemoryFolder parent, String name, Instant lastModified) {
		this.parent = parent;
		this.name = name;
		this.lastModified = lastModified;
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
		return parent.children().anyMatch(node -> node.equals(this));
	}

	@Override
	public Instant lastModified() {
		return lastModified;
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
					&& (this.parent == null && other.parent == null || this.parent.equals(other.parent)) //
					&& (this.name == null && other.name == null || this.name.equals(other.name));
		} else {
			return false;
		}
	}

}

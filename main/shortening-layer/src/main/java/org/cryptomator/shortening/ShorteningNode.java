package org.cryptomator.shortening;

import java.time.Instant;
import java.util.Optional;

import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

class ShorteningNode<E extends Node> implements Node {

	protected final E delegate;
	private final ShorteningFolder parent;
	private final String longName;
	private final String shortName;

	public ShorteningNode(ShorteningFolder parent, E delegate, String longName) {
		this.delegate = delegate;
		this.parent = parent;
		this.shortName = delegate.name();
		this.longName = longName;
	}

	@Override
	public String name() {
		return longName;
	}

	protected String shortName() {
		return shortName;
	}

	@Override
	public Optional<? extends Folder> parent() {
		return Optional.ofNullable(parent);
	}

	@Override
	public boolean exists() {
		return delegate.exists();
	}

	@Override
	public Instant lastModified() {
		return delegate.lastModified();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((longName == null) ? 0 : longName.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ShorteningNode) {
			ShorteningNode<?> other = (ShorteningNode<?>) obj;
			return this.getClass() == other.getClass() //
					&& (this.parent == null && other.parent == null || this.parent.equals(other.parent)) //
					&& (this.longName == null && other.longName == null || this.longName.equals(other.longName));
		} else {
			return false;
		}
	}

}

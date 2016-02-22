/*******************************************************************************
 * Copyright (c) 2015 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.filesystem.delegating;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Optional;

import org.cryptomator.filesystem.Node;

public abstract class DelegatingNode<T extends Node> implements Node {

	protected final T delegate;

	public DelegatingNode(T delegate) {
		if (delegate == null) {
			throw new IllegalArgumentException("Delegate must not be null");
		}
		this.delegate = delegate;
	}

	@Override
	public String name() throws UncheckedIOException {
		return delegate.name();
	}

	@Override
	public boolean exists() throws UncheckedIOException {
		return delegate.exists();
	}

	@Override
	public Instant lastModified() throws UncheckedIOException {
		return delegate.lastModified();
	}

	@Override
	public void setLastModified(Instant instant) throws UncheckedIOException {
		delegate.setLastModified(instant);
	}

	@Override
	public Optional<Instant> creationTime() throws UncheckedIOException {
		return delegate.creationTime();
	}

	@Override
	public void setCreationTime(Instant instant) throws UncheckedIOException {
		delegate.setCreationTime(instant);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DelegatingNode) {
			DelegatingNode<?> other = (DelegatingNode<?>) obj;
			return this.delegate.equals(other.delegate);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "Delegate[" + delegate + "]";
	}

}

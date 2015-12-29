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
import java.util.Optional;

import org.cryptomator.filesystem.File;

public abstract class DelegatingFile<R extends DelegatingReadableFile, W extends DelegatingWritableFile, D extends DelegatingFolder<R, W, D, ?>> extends DelegatingNode<File>implements File {

	private final D parent;

	public DelegatingFile(D parent, File delegate) {
		super(delegate);
		this.parent = parent;
	}

	@Override
	public Optional<D> parent() throws UncheckedIOException {
		return Optional.of(parent);
	}

	@Override
	public abstract R openReadable() throws UncheckedIOException;

	@Override
	public abstract W openWritable() throws UncheckedIOException;

	@Override
	public void copyTo(File destination) {
		if (getClass().equals(destination.getClass())) {
			final File delegateDest = ((DelegatingFile<?, ?, ?>) destination).delegate;
			delegate.copyTo(delegateDest);
		} else {
			delegate.copyTo(destination);
		}
	}

	@Override
	public void moveTo(File destination) {
		if (getClass().equals(destination.getClass())) {
			final File delegateDest = ((DelegatingFile<?, ?, ?>) destination).delegate;
			delegate.moveTo(delegateDest);
		} else {
			throw new IllegalArgumentException("Can only move DelegatingFile to other DelegatingFile.");
		}
	}

	@Override
	public int compareTo(File o) {
		if (getClass().equals(o.getClass())) {
			final File delegateOther = ((DelegatingFile<?, ?, ?>) o).delegate;
			return delegate.compareTo(delegateOther);
		} else {
			return delegate.compareTo(o);
		}
	}

}

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
import java.util.stream.Stream;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;
import org.cryptomator.filesystem.Node;

public abstract class DelegatingFolder<R extends DelegatingReadableFile, W extends DelegatingWritableFile, D extends DelegatingFolder<R, W, D, F>, F extends DelegatingFile<R, W, D>> extends DelegatingNode<Folder>
		implements Folder {

	private final D parent;

	public DelegatingFolder(D parent, Folder delegate) {
		super(delegate);
		this.parent = parent;
	}

	@Override
	public Optional<D> parent() throws UncheckedIOException {
		return Optional.ofNullable(parent);
	}

	@Override
	public Stream<? extends Node> children() throws UncheckedIOException {
		return Stream.concat(folders(), files());
	}

	@Override
	public Stream<D> folders() {
		return delegate.folders().map(this::folder);
	}

	@Override
	public Stream<F> files() throws UncheckedIOException {
		return delegate.files().map(this::file);
	}

	@Override
	public F file(String name) throws UncheckedIOException {
		return file(delegate.file(name));
	}

	protected abstract F file(File delegate);

	@Override
	public D folder(String name) throws UncheckedIOException {
		return folder(delegate.folder(name));
	}

	protected abstract D folder(Folder delegate);

	@Override
	public void create() throws UncheckedIOException {
		if (exists()) {
			return;
		}
		parent().ifPresent(p -> p.create());
		delegate.create();
	}

	@Override
	public void delete() {
		delegate.delete();
	}

	@Override
	public void copyTo(Folder destination) throws UncheckedIOException {
		if (destination instanceof DelegatingFolder) {
			final Folder delegateDest = ((DelegatingFolder<?, ?, ?, ?>) destination).delegate;
			delegate.copyTo(delegateDest);
		} else {
			delegate.copyTo(destination);
		}
	}

	@Override
	public void moveTo(Folder destination) {
		if (getClass().equals(destination.getClass())) {
			final Folder delegateDest = ((DelegatingFolder<?, ?, ?, ?>) destination).delegate;
			delegate.moveTo(delegateDest);
		} else {
			throw new IllegalArgumentException("Can only move DelegatingFolder to other DelegatingFolder.");
		}
	}

	@Override
	public void setCreationTime(Instant instant) throws UncheckedIOException {
		delegate.setCreationTime(instant);
	}

}

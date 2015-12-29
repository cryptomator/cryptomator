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
import java.util.stream.Stream;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.Folder;

public class DelegatingFolder extends DelegatingNode<Folder>implements Folder {

	public DelegatingFolder(DelegatingFolder parent, Folder delegate) {
		super(parent, delegate);
	}

	@Override
	public Stream<? extends DelegatingNode<?>> children() throws UncheckedIOException {
		return Stream.concat(folders(), files());
	}

	@Override
	public Stream<DelegatingFolder> folders() {
		return delegate.folders().map(this::folder);
	}

	@Override
	public Stream<DelegatingFile> files() throws UncheckedIOException {
		return delegate.files().map(this::file);
	}

	@Override
	public DelegatingFile file(String name) throws UncheckedIOException {
		return file(delegate.file(name));
	}

	private DelegatingFile file(File delegate) {
		return new DelegatingFile(this, delegate);
	}

	@Override
	public DelegatingFolder folder(String name) throws UncheckedIOException {
		return folder(delegate.folder(name));
	}

	private DelegatingFolder folder(Folder delegate) {
		return new DelegatingFolder(this, delegate);
	}

	@Override
	public void create() throws UncheckedIOException {
		delegate.create();
	}

	@Override
	public void delete() {
		delegate.delete();
	}

	@Override
	public void copyTo(Folder destination) throws UncheckedIOException {
		if (destination instanceof DelegatingFolder) {
			final Folder delegateDest = ((DelegatingFolder) destination).delegate;
			delegate.copyTo(delegateDest);
		} else {
			delegate.copyTo(destination);
		}
	}

	@Override
	public void moveTo(Folder destination) {
		if (getClass().equals(destination.getClass())) {
			final Folder delegateDest = ((DelegatingFolder) destination).delegate;
			delegate.moveTo(delegateDest);
		} else {
			throw new IllegalArgumentException("Can only move DelegatingFolder to other DelegatingFolder.");
		}
	}

}

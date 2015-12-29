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
import java.util.function.Function;

import org.cryptomator.filesystem.File;
import org.cryptomator.filesystem.ReadableFile;
import org.cryptomator.filesystem.WritableFile;

public class DelegatingFile extends DelegatingNode<File>implements File {

	private final Function<ReadableFile, DelegatingReadableFile> readableFileFactory;
	private final Function<WritableFile, DelegatingWritableFile> writableFileFactory;

	public DelegatingFile(DelegatingFolder parent, File delegate, Function<ReadableFile, DelegatingReadableFile> readableFileFactory, Function<WritableFile, DelegatingWritableFile> writableFileFactory) {
		super(parent, delegate);
		this.readableFileFactory = readableFileFactory;
		this.writableFileFactory = writableFileFactory;
	}

	@Override
	public DelegatingReadableFile openReadable() throws UncheckedIOException {
		return readableFileFactory.apply(delegate.openReadable());
	}

	@Override
	public DelegatingWritableFile openWritable() throws UncheckedIOException {
		return writableFileFactory.apply(delegate.openWritable());
	}

	@Override
	public void copyTo(File destination) {
		if (getClass().equals(destination.getClass())) {
			final File delegateDest = ((DelegatingFile) destination).delegate;
			delegate.copyTo(delegateDest);
		} else {
			delegate.copyTo(destination);
		}
	}

	@Override
	public void moveTo(File destination) {
		if (getClass().equals(destination.getClass())) {
			final File delegateDest = ((DelegatingFile) destination).delegate;
			delegate.moveTo(delegateDest);
		} else {
			throw new IllegalArgumentException("Can only move DelegatingFile to other DelegatingFile.");
		}
	}

	@Override
	public int compareTo(File o) {
		if (getClass().equals(o.getClass())) {
			final File delegateOther = ((DelegatingFile) o).delegate;
			return delegate.compareTo(delegateOther);
		} else {
			return delegate.compareTo(o);
		}
	}

}

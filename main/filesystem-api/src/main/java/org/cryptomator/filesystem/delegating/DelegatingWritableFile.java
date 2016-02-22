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
import java.nio.ByteBuffer;

import org.cryptomator.filesystem.WritableFile;

public class DelegatingWritableFile implements WritableFile {

	final WritableFile delegate;

	public DelegatingWritableFile(WritableFile delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public void truncate() throws UncheckedIOException {
		delegate.truncate();
	}

	@Override
	public int write(ByteBuffer source) throws UncheckedIOException {
		return delegate.write(source);
	}

	@Override
	public void position(long position) throws UncheckedIOException {
		delegate.position(position);
	}

	@Override
	public void close() throws UncheckedIOException {
		delegate.close();
	}

}

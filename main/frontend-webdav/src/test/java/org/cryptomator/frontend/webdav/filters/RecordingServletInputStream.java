/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.filters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import org.apache.commons.io.input.TeeInputStream;

class RecordingServletInputStream extends ServletInputStream {

	private final ServletInputStream delegate;
	private final TeeInputStream teeInputStream;
	private final ByteArrayOutputStream recording = new ByteArrayOutputStream(4096);

	public RecordingServletInputStream(ServletInputStream delegate) {
		this.delegate = delegate;
		this.teeInputStream = new TeeInputStream(delegate, recording);
	}

	public int read() throws IOException {
		return teeInputStream.read();
	}

	public int read(byte[] b) throws IOException {
		return teeInputStream.read(b);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return teeInputStream.read(b, off, len);
	}

	public boolean isFinished() {
		return delegate.isFinished();
	}

	public boolean isReady() {
		return delegate.isReady();
	}

	public void setReadListener(ReadListener readListener) {
		delegate.setReadListener(readListener);
	}

	public long skip(long n) throws IOException {
		return teeInputStream.skip(n);
	}

	public int available() throws IOException {
		return teeInputStream.available();
	}

	public void close() throws IOException {
		teeInputStream.close();
	}

	public byte[] getRecording() {
		return recording.toByteArray();
	}

	public void mark(int readlimit) {
	}

	public void reset() throws IOException {
		throw new IOException("Mark not supported");
	}

	public boolean markSupported() {
		return false;
	}

}

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

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.apache.commons.io.output.TeeOutputStream;

class RecordingServletOutputStream extends ServletOutputStream {

	private final ServletOutputStream delegate;
	private final TeeOutputStream teeOutputStream;
	private final ByteArrayOutputStream recording = new ByteArrayOutputStream(4096);

	public RecordingServletOutputStream(ServletOutputStream delegate) {
		this.delegate = delegate;
		this.teeOutputStream = new TeeOutputStream(delegate, recording);
	}

	public void write(int b) throws IOException {
		teeOutputStream.write(b);
	}

	public void write(byte[] b) throws IOException {
		teeOutputStream.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		teeOutputStream.write(b, off, len);
	}

	public void flush() throws IOException {
		teeOutputStream.flush();
	}

	public void close() throws IOException {
		teeOutputStream.close();
	}

	public boolean isReady() {
		return delegate.isReady();
	}

	public void setWriteListener(WriteListener writeListener) {
		delegate.setWriteListener(writeListener);
	}

	public byte[] getRecording() {
		return recording.toByteArray();
	}

}

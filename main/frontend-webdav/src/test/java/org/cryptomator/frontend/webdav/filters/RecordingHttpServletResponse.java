/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.filters;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

class RecordingHttpServletResponse extends HttpServletResponseWrapper {

	private final RecordingServletOutputStream recording;

	public RecordingHttpServletResponse(HttpServletResponse response) throws IOException {
		super(response);
		recording = new RecordingServletOutputStream(response.getOutputStream());
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return recording;
	}

	public byte[] getRecording() {
		return recording.getRecording();
	}

}

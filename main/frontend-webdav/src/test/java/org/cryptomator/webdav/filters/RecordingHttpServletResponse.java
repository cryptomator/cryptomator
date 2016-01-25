package org.cryptomator.webdav.filters;

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

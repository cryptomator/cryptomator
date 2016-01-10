package org.cryptomator.webdav.filters;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

class RecordingHttpServletRequest extends HttpServletRequestWrapper {

	private final RecordingServletInputStream recording;

	public RecordingHttpServletRequest(HttpServletRequest request) throws IOException {
		super(request);
		recording = new RecordingServletInputStream(request.getInputStream());
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return recording;
	}

	public byte[] getRecording() {
		return recording.getRecording();
	}

}

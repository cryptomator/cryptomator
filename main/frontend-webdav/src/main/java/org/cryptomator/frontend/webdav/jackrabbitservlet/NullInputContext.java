package org.cryptomator.frontend.webdav.jackrabbitservlet;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.jackrabbit.webdav.io.InputContext;

public class NullInputContext implements InputContext {

	@Override
	public boolean hasStream() {
		return true;
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(new byte[0]);
	}

	@Override
	public long getModificationTime() {
		return 0;
	}

	@Override
	public String getContentLanguage() {
		return null;
	}

	@Override
	public long getContentLength() {
		return 0;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public String getProperty(String propertyName) {
		return null;
	}

}

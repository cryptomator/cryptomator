/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.jackrabbit.webdav.io.InputContext;

class NullInputContext implements InputContext {

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

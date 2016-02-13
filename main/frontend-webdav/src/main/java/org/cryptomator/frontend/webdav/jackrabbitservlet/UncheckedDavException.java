/*******************************************************************************
 * Copyright (c) 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.frontend.webdav.jackrabbitservlet;

import org.apache.jackrabbit.webdav.DavException;

public class UncheckedDavException extends RuntimeException {

	private final int errorCode;

	public UncheckedDavException(int errorCode, String message) {
		this(errorCode, message, null);
	}

	public UncheckedDavException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	public DavException toDavException() {
		return new DavException(errorCode, getMessage(), getCause(), null);
	}

}

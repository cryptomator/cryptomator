package org.cryptomator.webdav.jackrabbitservlet;

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

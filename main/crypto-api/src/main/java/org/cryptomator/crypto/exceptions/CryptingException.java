package org.cryptomator.crypto.exceptions;

import java.io.IOException;

public class CryptingException extends IOException {
	private static final long serialVersionUID = -6622699014483319376L;

	public CryptingException(String string) {
		super(string);
	}

	public CryptingException(String string, Throwable t) {
		super(string, t);
	}
}
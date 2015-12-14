package org.cryptomator.crypto.engine;

import java.io.IOException;

public class CryptoException extends IOException {

	private static final long serialVersionUID = -6536997506620449023L;

	public CryptoException(String message, Throwable cause) {
		super(message, cause);
	}

}

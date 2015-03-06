package org.cryptomator.crypto.exceptions;

public class MacAuthenticationFailedException extends DecryptFailedException {

	private static final long serialVersionUID = -5577052361643658772L;

	public MacAuthenticationFailedException(String msg) {
		super(msg);
	}

}

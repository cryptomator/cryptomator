package org.cryptomator.crypto.exceptions;

public class WrongPasswordException extends StorageCryptingException {
	private static final long serialVersionUID = -602047799678568780L;

	public WrongPasswordException() {
		super("Wrong password.");
	}
}
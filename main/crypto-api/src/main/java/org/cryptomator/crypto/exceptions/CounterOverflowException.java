package org.cryptomator.crypto.exceptions;

public class CounterOverflowException extends EncryptFailedException {
	private static final long serialVersionUID = 380066751064534731L;

	public CounterOverflowException(String msg) {
		super(msg);
	}

}

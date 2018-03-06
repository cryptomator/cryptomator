package org.cryptomator.ui.model;

public class CommandFailedException extends Exception {

	public CommandFailedException(String message) {
		super(message);
	}

	public CommandFailedException(Throwable cause) {
		super(cause);
	}

	public CommandFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}

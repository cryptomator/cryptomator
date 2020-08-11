package org.cryptomator.common.mountpoint;

public class InvalidMountPointException extends Exception {

	public InvalidMountPointException(String message) {
		super(message);
	}

	public InvalidMountPointException(Throwable cause) {
		super(cause);
	}

	public InvalidMountPointException(String message, Throwable cause) {
		super(message, cause);
	}
}

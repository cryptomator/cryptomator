package org.cryptomator.common.vaults;

public class LockNotCompletedException extends Exception {

	public LockNotCompletedException(String reason) {
		super(reason);
	}

	public LockNotCompletedException(Throwable cause) {
		super(cause);
	}
}

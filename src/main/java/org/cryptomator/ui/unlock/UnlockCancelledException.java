package org.cryptomator.ui.unlock;

import org.cryptomator.cryptolib.api.MasterkeyLoadingFailedException;

public class UnlockCancelledException extends MasterkeyLoadingFailedException {

	public UnlockCancelledException(String message) {
		super(message);
	}

	public UnlockCancelledException(String message, Throwable cause) {
		super(message, cause);
	}
}

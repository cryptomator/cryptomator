package org.cryptomator.ui.preferences;

import java.util.concurrent.CompletionStage;

public interface AutoStartStrategy {

	CompletionStage<Boolean> isAutoStartEnabled();

	void enableAutoStart() throws TogglingAutoStartFailedException;

	void disableAutoStart() throws TogglingAutoStartFailedException;

	class TogglingAutoStartFailedException extends Exception {

		public TogglingAutoStartFailedException(String message) {
			super(message);
		}

		public TogglingAutoStartFailedException(String message, Throwable cause) {
			super(message, cause);
		}
		
	}
	class TogglingAutoStartWithPowershellFailedException extends TogglingAutoStartFailedException {

		public TogglingAutoStartWithPowershellFailedException(String message, Throwable cause) {
			super(message, cause);
		}

	}
}

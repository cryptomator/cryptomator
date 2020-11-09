package org.cryptomator.ui.preferences;

import java.util.concurrent.CompletionStage;

@Deprecated
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
}

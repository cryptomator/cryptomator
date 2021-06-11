package org.cryptomator.common.settings;


public class InvalidSettingsException extends RuntimeException {

	private final AbstractInvalidSetting reason;
	private final String additionalMessage;

	public InvalidSettingsException(AbstractInvalidSetting reason) {
		this(reason, null, null);
	}

	public InvalidSettingsException(AbstractInvalidSetting reason, String additionalMessage) {
		this(reason, additionalMessage, null);
	}

	public InvalidSettingsException(AbstractInvalidSetting reason, Throwable cause) {
		this(reason, null, cause);
	}

	public InvalidSettingsException(AbstractInvalidSetting reason, String additionalMessage, Throwable cause) {
		super(composeMessage(reason, additionalMessage), cause);
		this.reason = reason;
		this.additionalMessage = additionalMessage;
	}

	public AbstractInvalidSetting getReason() {
		return this.reason;
	}

	public String getAdditionalMessage() {
		return this.additionalMessage;
	}

	private static String composeMessage(AbstractInvalidSetting reason, String additionalMessage) {
		if (additionalMessage == null) {
			return reason.getMessage();
		}
		return reason.getMessage() + "\n" //
				+ "Additionally: " + additionalMessage;
	}
}
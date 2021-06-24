package org.cryptomator.common.settings;


public class InvalidSettingException extends RuntimeException {

	private final InvalidSetting reason;
	private final String additionalMessage;

	public InvalidSettingException(InvalidSetting reason) {
		this(reason, null, null);
	}

	public InvalidSettingException(InvalidSetting reason, String additionalMessage) {
		this(reason, additionalMessage, null);
	}

	public InvalidSettingException(InvalidSetting reason, Throwable cause) {
		this(reason, null, cause);
	}

	public InvalidSettingException(InvalidSetting reason, String additionalMessage, Throwable cause) {
		super(composeMessage(reason, additionalMessage), cause);
		this.reason = reason;
		this.additionalMessage = additionalMessage;
	}

	public InvalidSetting getReason() {
		return this.reason;
	}

	public String getAdditionalMessage() {
		return this.additionalMessage;
	}

	private static String composeMessage(InvalidSetting reason, String additionalMessage) {
		if (additionalMessage == null) {
			return reason.getMessage();
		}
		return reason.getMessage() + "\n" //
				+ "Additionally: " + additionalMessage;
	}
}
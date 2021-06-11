package org.cryptomator.common.settings;


public class InvalidSettingException extends RuntimeException {

	private final AbstractInvalidSetting reason;
	private final String additionalMessage;

	public InvalidSettingException(AbstractInvalidSetting reason) {
		this(reason, null, null);
	}

	public InvalidSettingException(AbstractInvalidSetting reason, String additionalMessage) {
		this(reason, additionalMessage, null);
	}

	public InvalidSettingException(AbstractInvalidSetting reason, Throwable cause) {
		this(reason, null, cause);
	}

	public InvalidSettingException(AbstractInvalidSetting reason, String additionalMessage, Throwable cause) {
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
package org.cryptomator.common.settings;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum WhenUnlocked {
	IGNORE("vaultOptions.general.actionAfterUnlock.ignore"),
	REVEAL("vaultOptions.general.actionAfterUnlock.reveal"),
	@JsonEnumDefaultValue ASK("vaultOptions.general.actionAfterUnlock.ask");

	private String displayName;

	WhenUnlocked(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}

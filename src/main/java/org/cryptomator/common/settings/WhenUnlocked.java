package org.cryptomator.common.settings;

public enum WhenUnlocked {
	IGNORE("vaultOptions.general.actionAfterUnlock.ignore"),
	REVEAL("vaultOptions.general.actionAfterUnlock.reveal"),
	ASK("vaultOptions.general.actionAfterUnlock.ask");

	private String displayName;

	WhenUnlocked(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}

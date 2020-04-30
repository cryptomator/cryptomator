package org.cryptomator.common.settings;

public enum UiTheme {
	LIGHT("preferences.general.theme.light"),
	DARK("preferences.general.theme.dark");
	// CUSTOM("Custom (%s)");

	private String displayName;

	UiTheme(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}

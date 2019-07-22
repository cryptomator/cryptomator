package org.cryptomator.common.settings;

public enum UiTheme {
	LIGHT("Light"),
	DARK("Dark"),
	CUSTOM("Custom (%s)");

	private String displayName;

	UiTheme(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}

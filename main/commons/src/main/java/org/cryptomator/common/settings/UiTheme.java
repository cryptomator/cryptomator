package org.cryptomator.common.settings;

import org.apache.commons.lang3.SystemUtils;

public enum UiTheme {
	LIGHT("preferences.general.theme.light"), //
	DARK("preferences.general.theme.dark"), //
	AUTOMATIC("preferences.general.theme.automatic");

	public static UiTheme[] applicableValues() {
		if (SystemUtils.IS_OS_MAC) {
			return values();
		} else {
			return new UiTheme[]{LIGHT, DARK};
		}
	}

	private final String displayName;

	UiTheme(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}

package org.cryptomator.common.settings;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.SystemUtils;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum UiTheme {
	@JsonEnumDefaultValue LIGHT("preferences.interface.theme.light"), //
	DARK("preferences.interface.theme.dark"), //
	AUTOMATIC("preferences.interface.theme.automatic");

	public static UiTheme[] applicableValues() {
		if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_WINDOWS) {
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

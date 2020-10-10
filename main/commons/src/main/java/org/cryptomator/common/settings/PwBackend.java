package org.cryptomator.common.settings;

import org.apache.commons.lang3.SystemUtils;

public enum PwBackend {
	GNOME("preferences.general.pwBackend.gnome"), //
	KDE("preferences.general.pwBackend.kde");

	public static PwBackend[] applicableValues() {
		if (SystemUtils.IS_OS_LINUX) {
			return values();
		} else {
			return new PwBackend[]{};
		}
	}

	private final String displayName;

	PwBackend(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}

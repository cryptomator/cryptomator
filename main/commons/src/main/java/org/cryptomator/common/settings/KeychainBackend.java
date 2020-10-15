package org.cryptomator.common.settings;

import org.apache.commons.lang3.SystemUtils;

import java.util.Arrays;

public enum KeychainBackend {
	GNOME("preferences.general.keychainBackend.gnome", SystemUtils.IS_OS_LINUX), //
	KDE("preferences.general.keychainBackend.kde", SystemUtils.IS_OS_LINUX), //
	MAC_SYSTEM_KEYCHAIN("preferences.general.keychainBackend.macSystemKeychain", SystemUtils.IS_OS_MAC), //
	WIN_SYSTEM_KEYCHAIN("preferences.general.keychainBackend.winSystemKeychain", SystemUtils.IS_OS_WINDOWS);

	public static KeychainBackend[] supportedBackends() {
		return Arrays.stream(values()).filter(KeychainBackend::isSupported).toArray(KeychainBackend[]::new);
	}
	public static KeychainBackend defaultBackend() {
		if (SystemUtils.IS_OS_LINUX) {
			return KeychainBackend.GNOME;
		} else { // SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_WINDOWS
			return Arrays.stream(KeychainBackend.supportedBackends()).findFirst().orElseThrow(IllegalStateException::new);
		}
	}

	private final String configName;
	private final boolean isSupported;

	KeychainBackend(String configName, boolean isSupported) {
		this.configName = configName;
		this.isSupported = isSupported;
	}

	public String getDisplayName() {
		return configName;
	}
	public boolean isSupported() { return isSupported; }

}

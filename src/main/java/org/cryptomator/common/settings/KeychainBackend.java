package org.cryptomator.common.settings;

public enum KeychainBackend {
	GNOME("org.cryptomator.linux.keychain.SecretServiceKeychainAccess"),
	KDE("org.cryptomator.linux.keychain.KDEWalletKeychainAccess"),
	MAC_SYSTEM_KEYCHAIN("org.cryptomator.macos.keychain.MacSystemKeychainAccess"),
	WIN_SYSTEM_KEYCHAIN("org.cryptomator.windows.keychain.WindowsProtectedKeychainAccess");

	private final String providerClass;

	KeychainBackend(String providerClass) {
		this.providerClass = providerClass;
	}

	public String getProviderClass() {
		return providerClass;
	}

}

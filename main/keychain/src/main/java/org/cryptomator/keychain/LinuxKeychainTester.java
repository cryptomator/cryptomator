package org.cryptomator.keychain;

import java.util.Optional;

public class LinuxKeychainTester {
	public static boolean secretServiceIsAvailable() {
		try {
			Class.forName("org.freedesktop.secret.simple.SimpleCollection");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public static Optional<GnomeKeyringAccess> getSecretService() {
		if (!secretServiceIsAvailable()) return Optional.empty();
		try {
			Class clazz = Class.forName("org.cryptomator.keychain.GnomeKeyringAccessImpl");
			GnomeKeyringAccess keyring = (GnomeKeyringAccess) clazz.getDeclaredConstructor().newInstance();
			return Optional.of(keyring);
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}

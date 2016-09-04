package org.cryptomator.keychain;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.jni.MacKeychainAccess;

class MacSystemKeychainAccess implements KeychainAccessStrategy {

	private final MacKeychainAccess keychain;

	@Inject
	public MacSystemKeychainAccess(Optional<MacFunctions> macFunctions) {
		if (macFunctions.isPresent()) {
			this.keychain = macFunctions.get().keychainAccess();
		} else {
			this.keychain = null;
		}
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		keychain.storePassword(key, passphrase);
	}

	@Override
	public char[] loadPassphrase(String key) {
		return keychain.loadPassword(key);
	}

	@Override
	public boolean isSupported() {
		return SystemUtils.IS_OS_MAC_OSX && keychain != null;
	}

	@Override
	public void deletePassphrase(String key) {
		keychain.deletePassword(key);
	}

}

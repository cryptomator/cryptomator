/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.jni.MacKeychainAccess;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
class MacSystemKeychainAccess implements KeychainAccessStrategy {

	private final Optional<MacFunctions> macFunctions;

	@Inject
	public MacSystemKeychainAccess(Optional<MacFunctions> macFunctions) {
		this.macFunctions = macFunctions;
	}

	private MacKeychainAccess keychain() {
		return macFunctions.orElseThrow(IllegalStateException::new).keychainAccess();
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		keychain().storePassword(key, passphrase);
	}

	@Override
	public char[] loadPassphrase(String key) {
		return keychain().loadPassword(key);
	}

	@Override
	public boolean isSupported() {
		return SystemUtils.IS_OS_MAC_OSX && macFunctions.isPresent();
	}

	@Override
	public void deletePassphrase(String key) {
		keychain().deletePassword(key);
	}

	@Override
	public void changePassphrase(String key, CharSequence passphrase) {
		if (keychain().deletePassword(key)) {
			keychain().storePassword(key, passphrase);
		}
	}

}

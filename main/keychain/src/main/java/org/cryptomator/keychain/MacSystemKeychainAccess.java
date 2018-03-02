/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.keychain;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.jni.MacKeychainAccess;

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

}

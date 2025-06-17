/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.common.keychain;

import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;

import java.util.HashMap;
import java.util.Map;

class MapKeychainAccess implements KeychainAccessProvider {

	private final Map<String, char[]> map = new HashMap<>();

	@Override
	public void storePassphrase(String key, String displayName,CharSequence passphrase) {
		char[] pw = new char[passphrase.length()];
		for (int i = 0; i < passphrase.length(); i++) {
			pw[i] = passphrase.charAt(i);
		}
		map.put(key, pw);
	}

	@Override
	public char[] loadPassphrase(String key) {
		return map.get(key);
	}

	@Override
	public void deletePassphrase(String key) {
		map.remove(key);
	}

	@Override
	public void changePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
		map.get(key);
		storePassphrase(key, displayName, passphrase);
	}

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public boolean isLocked() {
		return false;
	}

}

package org.cryptomator.keychain;

import java.util.HashMap;
import java.util.Map;

class MapKeychainAccess implements KeychainAccessStrategy {

	private final Map<String, char[]> map = new HashMap<>();

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
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
	public boolean isSupported() {
		return true;
	}

}

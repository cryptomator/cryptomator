package org.cryptomator.keychain;

import java.util.HashMap;
import java.util.Map;

class MapKeychainAccess implements KeychainAccessStrategy {

	private final Map<String, CharSequence> map = new HashMap<>();

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		map.put(key, passphrase);
	}

	@Override
	public CharSequence loadPassphrase(String key) {
		return map.get(key);
	}

	@Override
	public void deletePassphrase(String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSupported() {
		return true;
	}

}

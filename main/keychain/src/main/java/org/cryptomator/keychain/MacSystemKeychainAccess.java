package org.cryptomator.keychain;

import java.nio.CharBuffer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.jni.JniModule;
import org.cryptomator.jni.MacKeychainAccess;

@Singleton
class MacSystemKeychainAccess implements KeychainAccessStrategy {

	private final MacKeychainAccess keychain;

	@Inject
	public MacSystemKeychainAccess() {
		if (JniModule.macFunctions().isPresent()) {
			this.keychain = JniModule.macFunctions().get().getKeychainAccess();
		} else {
			this.keychain = null;
		}
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		keychain.storePassword(key, passphrase);
	}

	@Override
	public CharSequence loadPassphrase(String key) {
		return CharBuffer.wrap(keychain.loadPassword(key));
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

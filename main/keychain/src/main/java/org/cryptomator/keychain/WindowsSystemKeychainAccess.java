package org.cryptomator.keychain;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.SystemUtils;

@Singleton
class WindowsSystemKeychainAccess implements KeychainAccessStrategy {

	private final KeyStore keyStore;

	@Inject
	public WindowsSystemKeychainAccess() {
		KeyStore ks;
		try {
			ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
			ks.load(null);
		} catch (GeneralSecurityException | IOException e) {
			ks = null;
		}
		this.keyStore = ks;
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		// TODO Auto-generated method stub
	}

	@Override
	public char[] loadPassphrase(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deletePassphrase(String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSupported() {
		return SystemUtils.IS_OS_WINDOWS && keyStore != null;
	}

}

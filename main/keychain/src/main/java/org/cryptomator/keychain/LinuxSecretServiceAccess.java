package org.cryptomator.keychain;

import javax.inject.Inject;

public class LinuxSecretServiceAccess implements KeychainAccessStrategy {

	@Inject
	public LinuxSecretServiceAccess() {
	}

	@Override
	public boolean isSupported() {
		return false;
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {

	}

	@Override
	public char[] loadPassphrase(String key) {
		return null;
	}

	@Override
	public void deletePassphrase(String key) {

	}
}

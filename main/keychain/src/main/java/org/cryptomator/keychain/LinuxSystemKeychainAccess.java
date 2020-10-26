package org.cryptomator.keychain;

import org.apache.commons.lang3.SystemUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * A facade to LinuxSecretServiceKeychainAccessImpl and LinuxKDEWalletKeychainAccessImpl
 * that depend on libraries that are unavailable on Mac and Windows.
 */
@Singleton
public class LinuxSystemKeychainAccess implements KeychainAccessStrategy {

	// the actual implementation is hidden in this delegate objects which are loaded via reflection,
	// as it depends on libraries that aren't necessarily available:
	private final Optional<KeychainAccessStrategy> delegate;

	@Inject
	public LinuxSystemKeychainAccess() {
		this.delegate = constructKeychainAccess();
	}

	private static Optional<KeychainAccessStrategy> constructKeychainAccess() {
		try { // is gnome-keyring or kwallet installed?
			Class<?> clazz = Class.forName("org.cryptomator.keychain.LinuxSecretServiceKeychainAccessImpl");
			KeychainAccessStrategy instance = (KeychainAccessStrategy) clazz.getDeclaredConstructor().newInstance();
			if (instance.isSupported()) return Optional.of(instance);
			clazz = Class.forName("org.cryptomator.keychain.LinuxKDEWalletKeychainAccessImpl");
			instance = (KeychainAccessStrategy) clazz.getDeclaredConstructor().newInstance();
			return Optional.of(instance);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	@Override
	public boolean isSupported() {
		return SystemUtils.IS_OS_LINUX && delegate.map(KeychainAccessStrategy::isSupported).orElse(false);
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) throws KeychainAccessException {
		delegate.orElseThrow(IllegalStateException::new).storePassphrase(key, passphrase);
	}

	@Override
	public char[] loadPassphrase(String key) throws KeychainAccessException {
		return delegate.orElseThrow(IllegalStateException::new).loadPassphrase(key);
	}

	@Override
	public void deletePassphrase(String key) throws KeychainAccessException {
		delegate.orElseThrow(IllegalStateException::new).deletePassphrase(key);
	}

	@Override
	public void changePassphrase(String key, CharSequence passphrase) throws KeychainAccessException {
		delegate.orElseThrow(IllegalStateException::new).changePassphrase(key, passphrase);
	}
}

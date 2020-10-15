package org.cryptomator.keychain;

import javafx.beans.property.ObjectProperty;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.KeychainBackend;
import org.cryptomator.common.settings.Settings;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.Optional;

/**
 * A facade to LinuxSecretServiceKeychainAccessImpl and LinuxKDEWalletKeychainAccessImpl
 * that depend on libraries that are unavailable on Mac and Windows.
 */
@Singleton
public class LinuxSystemKeychainAccess implements KeychainAccessStrategy {

	// the actual implementation is hidden in this delegate object,
	// as on Linux the are two possible password backends available:
	private final Optional<KeychainAccessStrategy> delegate;
	private final Settings settings;
	private static EnumSet<KeychainBackend> availableKeychainBackends = EnumSet.noneOf(KeychainBackend.class);
	private static KeychainBackend backendActivated = null;
	private static boolean isGnomeKeyringAvailable;
	private static boolean isKdeWalletAvailable;

	@Inject
	public LinuxSystemKeychainAccess(Settings settings) {
		this.settings = settings;
		this.delegate = constructKeychainAccess();
	}

	private Optional<KeychainAccessStrategy> constructKeychainAccess() {
		try { // find out which backends are available
			KeychainAccessStrategy gnomeKeyring = new LinuxSecretServiceKeychainAccessImpl();
			if (gnomeKeyring.isSupported()) {
				LinuxSystemKeychainAccess.availableKeychainBackends.add(KeychainBackend.GNOME);
				LinuxSystemKeychainAccess.isGnomeKeyringAvailable = true;
			}
			KeychainAccessStrategy kdeWallet = new LinuxKDEWalletKeychainAccessImpl();
			if (kdeWallet.isSupported()) {
				LinuxSystemKeychainAccess.availableKeychainBackends.add(KeychainBackend.KDE);
				LinuxSystemKeychainAccess.isKdeWalletAvailable = true;
			}

			// load password backend setting as the preferred backend
			ObjectProperty<KeychainBackend> pwSetting =  settings.keychainBackend();

			// check for GNOME keyring first, as this gets precedence over
			// KDE wallet as the former was implemented first
			if (isGnomeKeyringAvailable && pwSetting.get().equals(KeychainBackend.GNOME)) {
					pwSetting.setValue(KeychainBackend.GNOME);
					LinuxSystemKeychainAccess.backendActivated = KeychainBackend.GNOME;
					return Optional.of(gnomeKeyring);
			}

			if (isKdeWalletAvailable && pwSetting.get().equals(KeychainBackend.KDE)) {
					pwSetting.setValue(KeychainBackend.KDE);
					LinuxSystemKeychainAccess.backendActivated = KeychainBackend.KDE;
					return Optional.of(kdeWallet);
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	/* Getter/Setter */

	public static EnumSet<KeychainBackend> getAvailableKeychainBackends() {
		return availableKeychainBackends;
	}

	public static KeychainBackend getBackendActivated() {
		return backendActivated;
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

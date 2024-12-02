package org.cryptomator.common.keychain;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.Arrays;

@Singleton
public class KeychainManager implements KeychainAccessProvider {

	private final ObjectExpression<KeychainAccessProvider> keychain;
	private final LoadingCache<String, BooleanProperty> passphraseStoredProperties;

	@Inject
	KeychainManager(ObjectExpression<KeychainAccessProvider> selectedKeychain) {
		this.keychain = selectedKeychain;
		this.passphraseStoredProperties = CacheBuilder.newBuilder() //
				.weakValues() //
				.build(CacheLoader.from(this::createStoredPassphraseProperty));
		keychain.addListener(ignored -> passphraseStoredProperties.invalidateAll());
	}

	private KeychainAccessProvider getKeychainOrFail() throws KeychainAccessException {
		var result = keychain.getValue();
		if (result == null) {
			throw new NoKeychainAccessProviderException();
		}
		return result;
	}

	@Override
	public String displayName() {
		return getClass().getName();
	}

	@Override
	public void storePassphrase(String key, String displayName, CharSequence passphrase, boolean requireOsAuthentication) throws KeychainAccessException {
		getKeychainOrFail().storePassphrase(key, displayName, passphrase, requireOsAuthentication);
		setPassphraseStored(key, true);
	}

	@Override
	public char[] loadPassphrase(String key) throws KeychainAccessException {
		char[] passphrase = getKeychainOrFail().loadPassphrase(key);
		setPassphraseStored(key, passphrase != null);
		return passphrase;
	}

	@Override
	public void deletePassphrase(String key) throws KeychainAccessException {
		getKeychainOrFail().deletePassphrase(key);
		setPassphraseStored(key, false);
	}

	@Override
	public void changePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
		if (isPassphraseStored(key)) {
			getKeychainOrFail().changePassphrase(key, displayName, passphrase);
			setPassphraseStored(key, true);
		}
	}

	@Override
	public boolean isSupported() {
		return keychain.getValue() != null;
	}

	@Override
	public boolean isLocked() {
		return keychain.getValue() == null || keychain.get().isLocked();
	}

	/**
	 * Checks if the keychain knows a passphrase for the given key.
	 * <p>
	 * Expensive operation. If possible, use {@link #getPassphraseStoredProperty(String)} instead.
	 *
	 * @param key The key to look up
	 * @return <code>true</code> if a password for <code>key</code> is stored.
	 * @throws KeychainAccessException
	 */
	public boolean isPassphraseStored(String key) throws KeychainAccessException {
		char[] storedPw = null;
		try {
			storedPw = getKeychainOrFail().loadPassphrase(key);
			return storedPw != null;
		} finally {
			if (storedPw != null) {
				Arrays.fill(storedPw, ' ');
			}
		}
	}

	private void setPassphraseStored(String key, boolean value) {
		BooleanProperty property = passphraseStoredProperties.getIfPresent(key);
		if (property != null) {
			if (Platform.isFxApplicationThread()) {
				property.set(value);
			} else {
				Platform.runLater(() -> property.set(value));
			}
		}
	}

	/**
	 * Returns an observable property for use in the UI that tells whether a passphrase is stored for the given key.
	 * <p>
	 * Assuming that this process is the only process modifying Cryptomator-related items in the system keychain, this
	 * property stays in memory in an attempt to avoid unnecessary calls to the system keychain. Note that due to this
	 * fact the value stored in the returned property is not 100% reliable. Code defensively!
	 *
	 * @param key The key to look up
	 * @return An observable property which is <code>true</code> when it almost certain that a password for <code>key</code> is stored.
	 * @see #isPassphraseStored(String)
	 */
	public ReadOnlyBooleanProperty getPassphraseStoredProperty(String key) {
		return passphraseStoredProperties.getUnchecked(key);
	}

	private BooleanProperty createStoredPassphraseProperty(String key) {
		try {
			return new SimpleBooleanProperty(isPassphraseStored(key));
		} catch (KeychainAccessException e) {
			return new SimpleBooleanProperty(false);
		}
	}

}

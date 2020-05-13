package org.cryptomator.keychain;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class KeychainManager implements KeychainAccessStrategy {

	private static final Logger LOG = LoggerFactory.getLogger(KeychainManager.class);

	private final KeychainAccessStrategy keychain;
	private LoadingCache<String, BooleanProperty> passphraseStoredProperties;

	KeychainManager(KeychainAccessStrategy keychain) {
		assert keychain.isSupported();
		this.keychain = keychain;
		this.passphraseStoredProperties = CacheBuilder.newBuilder() //
				.weakValues() //
				.build(CacheLoader.from(this::createStoredPassphraseProperty));
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) throws KeychainAccessException {
		keychain.storePassphrase(key, passphrase);
		setPassphraseStored(key, true);
	}

	@Override
	public char[] loadPassphrase(String key) throws KeychainAccessException {
		char[] passphrase = keychain.loadPassphrase(key);
		setPassphraseStored(key, passphrase != null);
		return passphrase;
	}

	@Override
	public void deletePassphrase(String key) throws KeychainAccessException {
		keychain.deletePassphrase(key);
		setPassphraseStored(key, false);
	}

	@Override
	public void changePassphrase(String key, CharSequence passphrase) throws KeychainAccessException {
		keychain.changePassphrase(key, passphrase);
		setPassphraseStored(key, true);
	}

	@Override
	public boolean isSupported() {
		return true;
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
			storedPw = keychain.loadPassphrase(key);
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
				LOG.warn("");
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
			LOG.warn("LOAD"); // TODO remove
			return new SimpleBooleanProperty(isPassphraseStored(key));
		} catch (KeychainAccessException e) {
			return new SimpleBooleanProperty(false);
		}
	}

}

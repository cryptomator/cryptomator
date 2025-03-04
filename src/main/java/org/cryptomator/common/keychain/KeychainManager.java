package org.cryptomator.common.keychain;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.cryptomator.common.Passphrase;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.integrations.keychain.KeychainAccessProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class KeychainManager implements KeychainAccessProvider {

	private static final Logger LOG = LoggerFactory.getLogger(KeychainManager.class);

	private final ObjectExpression<KeychainAccessProvider> keychain;
	private final LoadingCache<String, BooleanProperty> passphraseStoredProperties;
	private final ReentrantReadWriteLock lock;

	@Inject
	KeychainManager(ObjectExpression<KeychainAccessProvider> selectedKeychain) {
		this.keychain = selectedKeychain;
		this.passphraseStoredProperties = Caffeine.newBuilder() //
				.softValues() //
				.build(this::createStoredPassphraseProperty);
		keychain.addListener(ignored -> passphraseStoredProperties.invalidateAll());
		this.lock = new ReentrantReadWriteLock(false);
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
	public void storePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
		storePassphrase(key, displayName, passphrase, true);
	}

	//TODO: remove ignored parameter once the API is fixed
	@Override
	public void storePassphrase(String key, String displayName, CharSequence passphrase, boolean ignored) throws KeychainAccessException {
		try {
			lock.writeLock().lock();
			var kc = getKeychainOrFail();
			//this is the only keychain actually using the parameter
			var usesOSAuth = (kc.getClass().getName().equals("org.cryptomator.macos.keychain.TouchIdKeychainAccess"));
			kc.storePassphrase(key, displayName, passphrase, usesOSAuth);
		} finally {
			lock.writeLock().unlock();
		}
		setPassphraseStored(key, true);
	}

	@Override
	public char[] loadPassphrase(String key) throws KeychainAccessException {
		char[] passphrase = null;
		try {
			lock.readLock().lock();
			passphrase = getKeychainOrFail().loadPassphrase(key);
		} finally {
			lock.readLock().unlock();
		}
		setPassphraseStored(key, passphrase != null);
		return passphrase;
	}

	@Override
	public void deletePassphrase(String key) throws KeychainAccessException {
		try {
			lock.writeLock().lock();
			getKeychainOrFail().deletePassphrase(key);
		} finally {
			lock.writeLock().unlock();
		}
		setPassphraseStored(key, false);
	}

	@Override
	public void changePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
		if (isPassphraseStored(key)) {
			try {
				lock.writeLock().lock();
				getKeychainOrFail().changePassphrase(key, displayName, passphrase);
			} finally {
				lock.writeLock().unlock();
			}
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
		BooleanProperty property = passphraseStoredProperties.get(key, _ -> new SimpleBooleanProperty(value));
		if (Platform.isFxApplicationThread()) {
			property.set(value);
		} else {
			Platform.runLater(() -> property.set(value));
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
		return passphraseStoredProperties.get(key);
	}

	private BooleanProperty createStoredPassphraseProperty(String key) {
		try {
			return new SimpleBooleanProperty(isPassphraseStored(key));
		} catch (KeychainAccessException e) {
			return new SimpleBooleanProperty(false);
		}
	}

	public ObjectExpression<KeychainAccessProvider> getKeychainImplementation() {
		return this.keychain;
	}

	public static void migrate(KeychainAccessProvider oldProvider, KeychainAccessProvider newProvider, Map<String, String> idsAndNames) {
		if (oldProvider instanceof KeychainManager || newProvider instanceof KeychainManager) {
			throw new IllegalArgumentException("KeychainManger must not be the source or target of migration");
		}

		LOG.info("Migrating keychain entries {} from {} to {}", idsAndNames.keySet(), oldProvider.displayName(), newProvider.displayName());
		idsAndNames.forEach((id, name) -> {
			try {
				var passphrase = oldProvider.loadPassphrase(id);
				if (passphrase != null) {
					var wrapper = new Passphrase(passphrase);
					newProvider.storePassphrase(id, name, wrapper);
					oldProvider.deletePassphrase(id);
					wrapper.destroy();
				}
			} catch (KeychainAccessException e) {
				LOG.error("Failed to migrate keychain entry for vault {}.", id, e);
			}
		});
	}
}

package org.cryptomator.common.keychain;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.cryptomator.common.Passphrase;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class KeychainManager implements KeychainAccessProvider {

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
	public void storePassphrase(String key, String displayName, CharSequence passphrase) throws KeychainAccessException {
		try {
			lock.writeLock().lock();
			getKeychainOrFail().storePassphrase(key, displayName, passphrase);
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

	public static void migrate(KeychainAccessProvider oldProvider, KeychainAccessProvider newProvider, Map<String, String> idsAndNames) throws KeychainAccessException {
		if (oldProvider instanceof KeychainManager || newProvider instanceof KeychainManager) {
			throw new IllegalArgumentException("KeychainManger must not be the source or target of migration");
		}
		var migrated = new LinkedHashMap<String, String>();
		try {
			for (var entry : idsAndNames.entrySet()) {
				if (move(oldProvider, newProvider, entry.getKey(), entry.getValue())) {
					migrated.put(entry.getKey(), entry.getValue());
				}
			}
		} catch (KeychainAccessException e) {
			for (var entry : migrated.entrySet()) {
				try {
					move(newProvider, oldProvider, entry.getKey(), entry.getValue());
				} catch (KeychainAccessException rollbackException) {
					e.addSuppressed(rollbackException);
				}
			}
			throw e;
		}
	}

	private static boolean move(KeychainAccessProvider source, KeychainAccessProvider target, String key, String displayName) throws KeychainAccessException {
		var passphrase = source.loadPassphrase(key);
		if (passphrase == null) {
			return false;
		}
		var wrapper = new Passphrase(passphrase);
		try {
			source.deletePassphrase(key); // delete before storing: both providers may address the same keychain item (e.g., Touch ID and regular keychain on macOS), so deleting afterwards would remove the moved entry
			try {
				target.storePassphrase(key, displayName, wrapper);
			} catch (KeychainAccessException e) {
				try {
					source.storePassphrase(key, displayName, wrapper);
				} catch (KeychainAccessException restoreException) {
					e.addSuppressed(restoreException);
				}
				throw e;
			}
		} finally {
			wrapper.destroy();
		}
		return true;
	}
}

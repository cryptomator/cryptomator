package org.cryptomator.keychain;

import org.freedesktop.secret.simple.SimpleCollection;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class LinuxSecretServiceKeychainAccessImpl implements KeychainAccessStrategy {

	private final String LABEL_FOR_SECRET_IN_KEYRING = "Cryptomator";

	@Override
	public boolean isSupported() {
		try (@SuppressWarnings("unused") SimpleCollection keyring = new SimpleCollection()) {
			// seems like we're able to access the keyring.
			return true;
		} catch (IOException | RuntimeException e) {
			return false;
		}
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) throws KeychainAccessException {
		try (SimpleCollection keyring = new SimpleCollection()) {
			List<String> list = keyring.getItems(createAttributes(key));
			if (list == null) {
				keyring.createItem(LABEL_FOR_SECRET_IN_KEYRING, passphrase, createAttributes(key));
			}
		} catch (IOException e) {
			throw new KeychainAccessException(e);
		}
	}

	@Override
	public char[] loadPassphrase(String key) throws KeychainAccessException {
		try (SimpleCollection keyring = new SimpleCollection()) {
			List<String> list = keyring.getItems(createAttributes(key));
			if (list != null) {
				return keyring.getSecret(list.get(0));
			} else {
				return null;
			}
		} catch (IOException e) {
			throw new KeychainAccessException(e);
		}
	}

	@Override
	public void deletePassphrase(String key) throws KeychainAccessException {
		try (SimpleCollection keyring = new SimpleCollection()) {
			List<String> list = keyring.getItems(createAttributes(key));
			if (list != null) {
				keyring.deleteItem(list.get(0));
			}
		} catch (IOException e) {
			throw new KeychainAccessException(e);
		}
	}

	@Override
	public void changePassphrase(String key, CharSequence passphrase) throws KeychainAccessException {
		try (SimpleCollection keyring = new SimpleCollection()) {
			List<String> list = keyring.getItems(createAttributes(key));
			if (list != null) {
				keyring.updateItem(list.get(0), LABEL_FOR_SECRET_IN_KEYRING, passphrase, createAttributes(key));
			}
		} catch (IOException e) {
			throw new KeychainAccessException(e);
		}
	}

	private Map<String, String> createAttributes(String key) {
		Map<String, String> attributes = new HashMap();
		attributes.put("Vault", key);
		return attributes;
	}
}

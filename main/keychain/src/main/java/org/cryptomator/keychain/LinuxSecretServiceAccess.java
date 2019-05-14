package org.cryptomator.keychain;

import org.apache.commons.lang3.SystemUtils;
import org.freedesktop.secret.simple.SimpleCollection;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinuxSecretServiceAccess implements KeychainAccessStrategy {
	private SimpleCollection gnomeLoginKeyring = null;

	@Inject
	public LinuxSecretServiceAccess() {
		try {
			gnomeLoginKeyring = new SimpleCollection();
		} catch (Exception e) {
			// Accessing secret-service DBus API failed
		}
	}

	@Override
	public boolean isSupported() {
		return SystemUtils.IS_OS_LINUX && gnomeLoginKeyring != null;
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		List<String> list = gnomeLoginKeyring.getItems(createAttributes(key));
		if (list == null) {
			gnomeLoginKeyring.createItem("Cryptomator", passphrase, createAttributes(key));
		}
	}

	@Override
	public char[] loadPassphrase(String key) {
		List<String> list = gnomeLoginKeyring.getItems(createAttributes(key));
		if (list != null) {
			return gnomeLoginKeyring.getSecret(list.get(0));
		} else {
			return null;
		}
	}

	@Override
	public void deletePassphrase(String key) {
		List<String> list = gnomeLoginKeyring.getItems(createAttributes(key));
		if (list != null) {
			gnomeLoginKeyring.deleteItem(list.get(0));
		}
	}

	private Map<String, String> createAttributes(String key) {
		Map<String, String> attributes = new HashMap();
		attributes.put("Vault", key);
		return attributes;
	}
}

package org.cryptomator.keychain;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.SystemUtils;
import org.freedesktop.secret.simple.SimpleCollection;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LinuxSecretServiceAccess implements KeychainAccessStrategy {
	private final Optional<SimpleCollection> gnomeLoginKeyring;

	@Inject
	public LinuxSecretServiceAccess() {
		SimpleCollection keyring = null;
		try {
			keyring = new SimpleCollection();
		} catch (Exception e) {
			// Accessing secret-service DBus API failed
		} finally {
			gnomeLoginKeyring = Optional.ofNullable(keyring);
		}
	}

	@Override
	public boolean isSupported() {
		return SystemUtils.IS_OS_LINUX && gnomeLoginKeyring.isPresent();
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		Preconditions.checkState(gnomeLoginKeyring.isPresent());
		List<String> list = gnomeLoginKeyring.get().getItems(createAttributes(key));
		if (list == null) {
			gnomeLoginKeyring.get().createItem("Cryptomator", passphrase, createAttributes(key));
		}
	}

	@Override
	public char[] loadPassphrase(String key) {
		Preconditions.checkState(gnomeLoginKeyring.isPresent());
		List<String> list = gnomeLoginKeyring.get().getItems(createAttributes(key));
		if (list != null) {
			return gnomeLoginKeyring.get().getSecret(list.get(0));
		} else {
			return null;
		}
	}

	@Override
	public void deletePassphrase(String key) {
		Preconditions.checkState(gnomeLoginKeyring.isPresent());
		List<String> list = gnomeLoginKeyring.get().getItems(createAttributes(key));
		if (list != null) {
			gnomeLoginKeyring.get().deleteItem(list.get(0));
		}
	}

	private Map<String, String> createAttributes(String key) {
		Map<String, String> attributes = new HashMap();
		attributes.put("Vault", key);
		return attributes;
	}
}

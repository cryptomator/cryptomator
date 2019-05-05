package org.cryptomator.keychain;

import org.apache.commons.lang3.SystemUtils;
import org.freedesktop.dbus.ObjectPath;
import org.freedesktop.secret.simple.SimpleCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinuxSecretServiceAccess implements KeychainAccessStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(LinuxSecretServiceAccess.class);
	private SimpleCollection gnomeLoginKeyring;

	@Inject
	public LinuxSecretServiceAccess() {
		gnomeLoginKeyring = new SimpleCollection();
	}

	@Override
	public boolean isSupported() {
		return SystemUtils.IS_OS_LINUX;
	}

	@Override
	public void storePassphrase(String key, CharSequence passphrase) {
		List<ObjectPath> list = gnomeLoginKeyring.getSecretByAttributes(createAttributes(key));
		if (list.isEmpty()) {
			gnomeLoginKeyring.createPassword("Cryptomator", passphrase, createAttributes(key));
		}
	}

	@Override
	public char[] loadPassphrase(String key) {
		List<ObjectPath> list = gnomeLoginKeyring.getSecretByAttributes(createAttributes(key));
		if (!list.isEmpty()) {
			return gnomeLoginKeyring.getPassword(list.get(0).getPath());
		} else {
			return null;
		}
	}

	@Override
	public void deletePassphrase(String key) {
		List<ObjectPath> list = gnomeLoginKeyring.getSecretByAttributes(createAttributes(key));
		if (!list.isEmpty()) {
			gnomeLoginKeyring.deletePassword(list.get(0).getPath());
		}
	}

	private Map<String, String> createAttributes(String key) {
		Map<String, String> attributes = new HashMap();
		attributes.put("Vault", key);
		return attributes;
	}
}

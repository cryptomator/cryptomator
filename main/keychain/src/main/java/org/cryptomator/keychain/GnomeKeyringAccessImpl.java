package org.cryptomator.keychain;

import org.freedesktop.secret.simple.SimpleCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GnomeKeyringAccessImpl implements GnomeKeyringAccess {
	private static final Logger LOG = LoggerFactory.getLogger(GnomeKeyringAccessImpl.class);
	private SimpleCollection keyring;
	private boolean dbusProblem = false;

	public GnomeKeyringAccessImpl() {
		try {
			keyring = new SimpleCollection();
		} catch (IOException e) {
			dbusProblem = true;
		}
	}

	public void storePassword(String key, CharSequence passphrase) {
		List<String> list = keyring.getItems(createAttributes(key));
		if (list == null) {
			keyring.createItem("Cryptomator", passphrase, createAttributes(key));
		}
	}

	public char[] loadPassword(String key) {
		List<String> list = keyring.getItems(createAttributes(key));
		if (list != null) {
			return keyring.getSecret(list.get(0));
		} else {
			return null;
		}
	}

	public void deletePassword(String key) {
		List<String> list = keyring.getItems(createAttributes(key));
		if (list != null) {
			keyring.deleteItem(list.get(0));
		}
	}

	private Map<String, String> createAttributes(String key) {
		Map<String, String> attributes = new HashMap();
		attributes.put("Vault", key);
		return attributes;
	}

	public boolean isDbusProblem() {
		return dbusProblem;
	}
}

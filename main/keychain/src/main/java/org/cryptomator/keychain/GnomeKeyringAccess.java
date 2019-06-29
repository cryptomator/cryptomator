package org.cryptomator.keychain;

public interface GnomeKeyringAccess {
	public void storePassword(String key, CharSequence passphrase);

	public char[] loadPassword(String key);

	public void deletePassword(String key);
}

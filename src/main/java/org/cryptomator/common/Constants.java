package org.cryptomator.common;

public interface Constants {

	String MASTERKEY_FILENAME = "masterkey.cryptomator";
	String MASTERKEY_BACKUP_SUFFIX = ".bkup";
	String VAULTCONFIG_FILENAME = "vault.cryptomator";
	byte[] PEPPER = new byte[0];
	int UNLIMITED_CLEARTEXT_FILENAME_LENGTH = Integer.MAX_VALUE;
	int UNKNOWN_CLEARTEXT_FILENAME_LENGTH_LIMIT = -1;

}

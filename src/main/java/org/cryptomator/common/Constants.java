package org.cryptomator.common;

public interface Constants {

	String MASTERKEY_FILENAME = "masterkey.cryptomator";
	String MASTERKEY_BACKUP_SUFFIX = ".bkup";
	String VAULTCONFIG_FILENAME = "vault.cryptomator";
	String CRYPTOMATOR_FILENAME_EXT = ".cryptomator";
	String CRYPTOMATOR_FILENAME_GLOB = "*.cryptomator";
	byte[] PEPPER = new byte[0];

	//TODO: DOC base URL hier hinzufügen und dann abhängig von der Version verlinken (version gibts aus environment)

}

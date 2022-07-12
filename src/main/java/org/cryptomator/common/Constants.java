package org.cryptomator.common;

import java.net.URI;

public interface Constants {

	String MASTERKEY_FILENAME = "masterkey.cryptomator";
	String MASTERKEY_BACKUP_SUFFIX = ".bkup";
	String VAULTCONFIG_FILENAME = "vault.cryptomator";
	String CRYPTOMATOR_FILENAME_EXT = ".cryptomator";
	String CRYPTOMATOR_FILENAME_GLOB = "*.cryptomator";
	byte[] PEPPER = new byte[0];
	URI DOC_BASE_URL = URI.create("https://docs.cryptomator.org/");

}

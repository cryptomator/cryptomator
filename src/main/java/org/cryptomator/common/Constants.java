package org.cryptomator.common;

import org.cryptomator.ui.keyloading.masterkeyfile.MasterkeyFileLoadingStrategy;

import java.net.URI;

public interface Constants {

	String MASTERKEY_FILENAME = "masterkey.cryptomator";
	String MASTERKEY_BACKUP_SUFFIX = ".bkup";
	String VAULTCONFIG_FILENAME = "vault.cryptomator";
	String CRYPTOMATOR_FILENAME_EXT = ".cryptomator";
	String CRYPTOMATOR_FILENAME_GLOB = "*.cryptomator";
	URI DEFAULT_KEY_ID = URI.create(MasterkeyFileLoadingStrategy.SCHEME + ":" + MASTERKEY_FILENAME);
	byte[] PEPPER = new byte[0];

}

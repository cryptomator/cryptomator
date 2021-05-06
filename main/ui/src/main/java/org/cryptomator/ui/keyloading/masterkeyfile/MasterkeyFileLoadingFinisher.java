package org.cryptomator.ui.keyloading.masterkeyfile;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
class MasterkeyFileLoadingFinisher {

	private static final Logger LOG = LoggerFactory.getLogger(MasterkeyFileLoadingFinisher.class);

	private final Vault vault;
	private final Optional<char[]> storedPassword;
	private final AtomicReference<char[]> enteredPassword;
	private final AtomicBoolean shouldSavePassword;
	private final KeychainManager keychain;

	@Inject
	MasterkeyFileLoadingFinisher(@KeyLoading Vault vault, @Named("savedPassword") Optional<char[]> storedPassword, AtomicReference<char[]> enteredPassword, @Named("savePassword") AtomicBoolean shouldSavePassword, KeychainManager keychain) {
		this.vault = vault;
		this.storedPassword = storedPassword;
		this.enteredPassword = enteredPassword;
		this.shouldSavePassword = shouldSavePassword;
		this.keychain = keychain;
	}

	public void cleanup(boolean successfullyUnlocked) {
		if (successfullyUnlocked && shouldSavePassword.get()) {
			savePasswordToSystemkeychain();
		}
		wipePassword(storedPassword.orElse(null));
		wipePassword(enteredPassword.getAndSet(null));
	}

	private void savePasswordToSystemkeychain() {
		if (keychain.isSupported()) {
			try {
				keychain.storePassphrase(vault.getId(), CharBuffer.wrap(enteredPassword.get()));
			} catch (KeychainAccessException e) {
				LOG.error("Failed to store passphrase in system keychain.", e);
			}
		}
	}

	private void wipePassword(char[] pw) {
		if (pw != null) {
			Arrays.fill(pw, ' ');
		}
	}
}

package org.cryptomator.ui.model;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.frontend.webdav.mount.Mounter.CommandFailedException;
import org.cryptomator.keychain.KeychainAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AutoUnlocker {

	private static final Logger LOG = LoggerFactory.getLogger(AutoUnlocker.class);

	private final Optional<KeychainAccess> keychainAccess;
	private final VaultList vaults;
	private final ExecutorService executor;

	@Inject
	public AutoUnlocker(Optional<KeychainAccess> keychainAccess, VaultList vaults, ExecutorService executor) {
		this.keychainAccess = keychainAccess;
		this.vaults = vaults;
		this.executor = executor;
	}

	public void unlockAllSilently() {
		if (keychainAccess.isPresent()) {
			vaults.stream().filter(this::shouldUnlockAfterStartup).map(this::createUnlockTask).forEach(executor::submit);
		}
	}

	private boolean shouldUnlockAfterStartup(Vault vault) {
		return vault.getVaultSettings().unlockAfterStartup().get();
	}

	private Runnable createUnlockTask(Vault vault) {
		return () -> unlockSilently(vault);
	}

	private void unlockSilently(Vault vault) {
		char[] storedPw = keychainAccess.get().loadPassphrase(vault.getId());
		if (storedPw == null) {
			LOG.warn("No passphrase stored in keychain for vault registered for auto unlocking: {}", vault.getPath());
		}
		try {
			vault.unlock(CharBuffer.wrap(storedPw));
			mountSilently(vault);
		} catch (CryptoException e) {
			LOG.error("Auto unlock failed.", e);
		} finally {
			Arrays.fill(storedPw, ' ');
		}
	}

	private void mountSilently(Vault unlockedVault) {
		if (!unlockedVault.getVaultSettings().mountAfterUnlock().get()) {
			return;
		}
		try {
			unlockedVault.mount();
			revealSilently(unlockedVault);
		} catch (CommandFailedException e) {
			LOG.error("Auto unlock succeded, but mounting the drive failed.", e);
		}
	}

	private void revealSilently(Vault mountedVault) {
		if (!mountedVault.getVaultSettings().revealAfterMount().get()) {
			return;
		}
		try {
			mountedVault.reveal();
		} catch (CommandFailedException e) {
			LOG.error("Auto unlock succeded, but revealing the drive failed.", e);
		}
	}

}

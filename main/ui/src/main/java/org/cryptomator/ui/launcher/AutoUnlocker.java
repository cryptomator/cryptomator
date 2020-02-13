package org.cryptomator.ui.launcher;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.common.vaults.Volume;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.keychain.KeychainAccessException;
import org.cryptomator.keychain.KeychainModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Optional;

@Singleton
class AutoUnlocker {
	
	private static final Logger LOG = LoggerFactory.getLogger(AutoUnlocker.class);

	private final ObservableList<Vault> vaults;
	private final Optional<KeychainAccess> keychain;

	@Inject
	AutoUnlocker(ObservableList<Vault> vaults, Optional<KeychainAccess> keychain) {
		this.vaults = vaults;
		this.keychain = keychain;
	}

	/**
	 * Attempts to unlock all vaults that have been configured for auto unlock.
	 * If an attempt fails (i.e. because the stored password is wrong) it will be silently skipped.
	 */
	public void autoUnlock() {
		if (!keychain.isPresent()) {
			LOG.info("No system keychain found. Skipping auto unlock.");
			return;
		}
		// TODO: do async
		vaults.filtered(v -> v.getVaultSettings().unlockAfterStartup().get()).forEach(this::autoUnlock);
	}

	private void autoUnlock(Vault vault) {
		if (vault.getState() != VaultState.LOCKED) {
			LOG.warn("Can't unlock vault {} due to its state {}", vault.getDisplayablePath(), vault.getState());
			return;
		}
		assert keychain.isPresent();
		char[] storedPw = null;
		try {
			storedPw = keychain.get().loadPassphrase(vault.getId());
			if (storedPw == null) {
				LOG.warn("No passphrase stored in keychain for vault registered for auto unlocking: {}", vault.getPath());
			} else {
				vault.unlock(CharBuffer.wrap(storedPw));
				// TODO
				// Platform.runLater(() -> vault.setState(VaultState.UNLOCKED));
				LOG.info("Unlocked vault {}", vault.getDisplayablePath());
			}
		} catch (IOException | Volume.VolumeException | KeychainAccessException e) {
			LOG.error("Auto unlock failed.", e);
		} finally {
			if (storedPw != null) {
				Arrays.fill(storedPw, ' ');
			}
		}
	}

}

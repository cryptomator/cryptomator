package org.cryptomator.common.recovery;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultComponent;
import org.cryptomator.common.vaults.VaultConfigCache;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.integrations.mount.MountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

import static org.cryptomator.common.vaults.VaultState.Value.LOCKED;

public final class VaultPreparator {

	private static final Logger LOG = LoggerFactory.getLogger(VaultPreparator.class);

	private VaultPreparator() {}

	public static Vault prepareVault(Path selectedDirectory, //
									 VaultComponent.Factory vaultComponentFactory, //
									 List<MountService> mountServices, //
									 ResourceBundle resourceBundle) {
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		vaultSettings.path.set(selectedDirectory);
		if (selectedDirectory.getFileName() != null) {
			vaultSettings.displayName.set(selectedDirectory.getFileName().toString());
		} else {
			vaultSettings.displayName.set(resourceBundle.getString("defaults.vault.vaultName"));
		}

		var wrapper = new VaultConfigCache(vaultSettings);
		Vault vault = vaultComponentFactory.create(vaultSettings, wrapper, LOCKED, null).vault();
		try {
			VaultListManager.determineVaultState(vault.getPath());
		} catch (IOException e) {
			LOG.warn("Failed to determine vault state for {}", vaultSettings.path.get(), e);
		}

		//due to https://github.com/cryptomator/cryptomator/issues/2880#issuecomment-1680313498
		var nameOfWinfspLocalMounter = "org.cryptomator.frontend.fuse.mount.WinFspMountProvider";
		if (SystemUtils.IS_OS_WINDOWS && vaultSettings.path.get().toString().contains("Dropbox") && mountServices.stream().anyMatch(s -> s.getClass().getName().equals(nameOfWinfspLocalMounter))) {
			vaultSettings.mountService.setValue(nameOfWinfspLocalMounter);
		}

		return vault;
	}
}

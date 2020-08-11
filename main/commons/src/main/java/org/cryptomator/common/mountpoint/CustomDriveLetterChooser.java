package org.cryptomator.common.mountpoint;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class CustomDriveLetterChooser implements MountPointChooser {

	private final VaultSettings vaultSettings;

	public CustomDriveLetterChooser(Vault vault) {
		this.vaultSettings = vault.getVaultSettings();
	}

	@Override
	public boolean isApplicable() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	@Override
	public Optional<Path> chooseMountPoint() {
		return this.vaultSettings.getWinDriveLetter().map(letter -> letter.charAt(0) + ":\\").map(Paths::get);
	}
}

package org.cryptomator.common.mountpoint;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class CustomDriveLetterChooser implements MountPointChooser {

	private final Vault vault;

	public CustomDriveLetterChooser(Vault vault) {
		this.vault = vault;
	}

	@Override
	public boolean isApplicable() {
		return SystemUtils.IS_OS_WINDOWS;
	}

	@Override
	public Optional<Path> chooseMountPoint() {
		return this.vault.getVaultSettings().getWinDriveLetter().map(letter -> letter.charAt(0) + ":\\").map(Paths::get);
	}
}

package org.cryptomator.common.mountpoint;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Volume;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class CustomDriveLetterChooser implements MountPointChooser {

	public static final int PRIORITY = 100;

	private final VaultSettings vaultSettings;

	@Inject
	public CustomDriveLetterChooser(VaultSettings vaultSettings) {
		this.vaultSettings = vaultSettings;
	}

	@Override
	public boolean isApplicable(Volume caller) {
		return SystemUtils.IS_OS_WINDOWS;
	}

	@Override
	public Optional<Path> chooseMountPoint(Volume caller) {
		return this.vaultSettings.getWinDriveLetter().map(letter -> letter.charAt(0) + ":\\").map(Paths::get);
	}

	@Override
	public int getPriority() {
		return PRIORITY;
	}
}

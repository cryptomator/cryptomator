package org.cryptomator.common.mountpoint;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Volume;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Optional;

class MacVolumeMountChooser implements MountPointChooser {

	private static final Path VOLUME_PATH = Path.of("/Volumes");

	private final VaultSettings vaultSettings;
	private final MountPointHelper helper;

	@Inject
	public MacVolumeMountChooser(VaultSettings vaultSettings, MountPointHelper helper) {
		this.vaultSettings = vaultSettings;
		this.helper = helper;
	}

	@Override
	public boolean isApplicable(Volume caller) {
		return SystemUtils.IS_OS_MAC;
	}

	@Override
	public Optional<Path> chooseMountPoint(Volume caller) {
		return Optional.of(helper.chooseTemporaryMountPoint(vaultSettings, VOLUME_PATH));
	}

	@Override
	public boolean prepare(Volume caller, Path mountPoint) {
		// https://github.com/osxfuse/osxfuse/issues/306#issuecomment-245114592:
		// In order to allow non-admin users to mount FUSE volumes in `/Volumes`,
		// starting with version 3.5.0, FUSE will create non-existent mount points automatically.
		// Therefore we don't need to prepare anything.
		return false;
	}
}

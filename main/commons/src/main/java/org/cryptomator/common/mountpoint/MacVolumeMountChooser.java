package org.cryptomator.common.mountpoint;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

class MacVolumeMountChooser implements MountPointChooser {

	private static final Logger LOG = LoggerFactory.getLogger(MacVolumeMountChooser.class);
	private static final int MAX_MOUNTPOINT_CREATION_RETRIES = 10;
	private static final Path VOLUME_PATH = Path.of("/Volumes");

	private final VaultSettings vaultSettings;

	@Inject
	public MacVolumeMountChooser(VaultSettings vaultSettings) {
		this.vaultSettings = vaultSettings;
	}

	@Override
	public boolean isApplicable(Volume caller) {
		return SystemUtils.IS_OS_MAC;
	}

	@Override
	public Optional<Path> chooseMountPoint(Volume caller) {
		String basename = this.vaultSettings.mountName().get();
		// regular
		Path mountPoint = VOLUME_PATH.resolve(basename);
		if (Files.notExists(mountPoint)) {
			return Optional.of(mountPoint);
		}
		// with id
		mountPoint = VOLUME_PATH.resolve(basename + " (" + vaultSettings.getId() + ")");
		if (Files.notExists(mountPoint)) {
			return Optional.of(mountPoint);
		}
		// with id and count
		for (int i = 1; i < MAX_MOUNTPOINT_CREATION_RETRIES; i++) {
			mountPoint = VOLUME_PATH.resolve(basename + "_(" + vaultSettings.getId() + ")_" + i);
			if (Files.notExists(mountPoint)) {
				return Optional.of(mountPoint);
			}
		}
		LOG.error("Failed to find feasible mountpoint at /Volumes/{}_x. Giving up after {} attempts.", basename, MAX_MOUNTPOINT_CREATION_RETRIES);
		return Optional.empty();
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

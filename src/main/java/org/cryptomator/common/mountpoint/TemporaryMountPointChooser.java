package org.cryptomator.common.mountpoint;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

class TemporaryMountPointChooser implements MountPointChooser {

	private static final Logger LOG = LoggerFactory.getLogger(TemporaryMountPointChooser.class);

	private final VaultSettings vaultSettings;
	private final Environment environment;
	private final MountPointHelper helper;

	@Inject
	public TemporaryMountPointChooser(VaultSettings vaultSettings, Environment environment, MountPointHelper helper) {
		this.vaultSettings = vaultSettings;
		this.environment = environment;
		this.helper = helper;
	}

	@Override
	public boolean isApplicable(Volume caller) {
		if (this.environment.getMountPointsDir().isEmpty()) {
			LOG.warn("\"cryptomator.mountPointsDir\" is not set to a valid path!");
			return false;
		}
		return true;
	}

	@Override
	public Optional<Path> chooseMountPoint(Volume caller) {
		assert environment.getMountPointsDir().isPresent();
		//clean leftovers of not-regularly unmounted vaults
		//see https://github.com/cryptomator/cryptomator/issues/1013 and https://github.com/cryptomator/cryptomator/issues/1061
		helper.clearIrregularUnmountDebrisIfNeeded();
		return this.environment.getMountPointsDir().map(dir -> this.helper.chooseTemporaryMountPoint(this.vaultSettings, dir));
	}

	@Override
	public boolean prepare(Volume caller, Path mountPoint) throws InvalidMountPointException {
		try {
			switch (caller.getMountPointRequirement()) {
				case PARENT_NO_MOUNT_POINT -> {
					Files.createDirectories(mountPoint.getParent());
					LOG.debug("Successfully created folder for mount point: {}", mountPoint);
					return false;
				}
				case EMPTY_MOUNT_POINT -> {
					Files.createDirectories(mountPoint);
					LOG.debug("Successfully created mount point: {}", mountPoint);
					return true;
				}
				case NONE -> {
					//Requirement "NONE" doesn't make any sense here.
					//No need to prepare/verify a Mountpoint without requiring one...
					throw new InvalidMountPointException(new IllegalStateException("Illegal MountPointRequirement"));
				}
				default -> {
					//Currently the case for "PARENT_OPT_MOUNT_POINT"
					throw new InvalidMountPointException(new IllegalStateException("Not implemented"));
				}
			}
		} catch (IOException exception) {
			throw new InvalidMountPointException("IOException while preparing mountpoint", exception);
		}
	}

	@Override
	public void cleanup(Volume caller, Path mountPoint) {
		try {
			Files.delete(mountPoint);
			LOG.debug("Successfully deleted mount point: {}", mountPoint);
		} catch (IOException e) {
			LOG.warn("Could not delete mount point: {}", e.getMessage());
		}
	}

}

package org.cryptomator.common.mountpoint;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.vaults.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class TemporaryMountPointChooser implements MountPointChooser {

	private static final Logger LOG = LoggerFactory.getLogger(TemporaryMountPointChooser.class);
	private static final int MAX_TMPMOUNTPOINT_CREATION_RETRIES = 10;

	private final Vault vault;
	private final Environment environment;

	public TemporaryMountPointChooser(Vault vault, Environment environment) {
		this.vault = vault;
		this.environment = environment;
	}

	@Override
	public boolean isApplicable() {
		if(this.environment.getMountPointsDir().isEmpty()) {
			LOG.warn("\"cryptomator.mountPointsDir\" is not set to a valid path!");
			return false;
		}
		return true;
	}

	@Override
	public Optional<Path> chooseMountPoint() {
		//Shouldn't throw, but let's keep #orElseThrow in case we made a mistake and the check in #isApplicable failed
		Path parent = this.environment.getMountPointsDir().orElseThrow();
		String basename = this.vault.getVaultSettings().getId();
		for (int i = 0; i < MAX_TMPMOUNTPOINT_CREATION_RETRIES; i++) {
			Path mountPoint = parent.resolve(basename + "_" + i);
			if (Files.notExists(mountPoint)) {
				return Optional.of(mountPoint);
			}
		}
		LOG.error("Failed to find feasible mountpoint at {}{}{}_x. Giving up after {} attempts.", parent, File.separator, basename, MAX_TMPMOUNTPOINT_CREATION_RETRIES);
		return Optional.empty();
	}

	@Override
	public boolean prepare(Path mountPoint) throws InvalidMountPointException {
		// https://github.com/osxfuse/osxfuse/issues/306#issuecomment-245114592:
		// In order to allow non-admin users to mount FUSE volumes in `/Volumes`,
		// starting with version 3.5.0, FUSE will create non-existent mount points automatically.
		if (SystemUtils.IS_OS_MAC && mountPoint.getParent().equals(Paths.get("/Volumes"))) {
			return false;
		}

		try {
			//WinFSP needs the parent, but the actual Mountpoint must not exist...
			if (SystemUtils.IS_OS_WINDOWS) {
				Files.createDirectories(mountPoint.getParent());
				LOG.debug("Successfully created folder for mount point: {}", mountPoint);
				return false;
			} else {
				//For Linux and Mac the actual Mountpoint must exist
				Files.createDirectories(mountPoint);
				LOG.debug("Successfully created mount point: {}", mountPoint);
				return true;
			}
		} catch (IOException exception) {
			throw wrapAsIMPE(exception);
		}
	}

	@Override
	public void cleanup(Path mountPoint) {
		try {
			Files.delete(mountPoint);
			LOG.debug("Successfully deleted mount point: {}", mountPoint);
		} catch (IOException e) {
			LOG.warn("Could not delete mount point: {}", e.getMessage());
		}
	}

	private InvalidMountPointException wrapAsIMPE(Exception exception) {
		return new InvalidMountPointException(exception);
	}
}

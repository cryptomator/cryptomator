package org.cryptomator.common.mountpoint;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.common.vaults.MountPointRequirement;
import org.cryptomator.common.vaults.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

class CustomMountPointChooser implements MountPointChooser {

	private static final String HIDEAWAY_PREFIX = ".~$";
	private static final String HIDEAWAY_SUFFIX = ".tmp";
	private static final Logger LOG = LoggerFactory.getLogger(CustomMountPointChooser.class);

	private final VaultSettings vaultSettings;
	private final Environment environment;

	@Inject
	public CustomMountPointChooser(VaultSettings vaultSettings, Environment environment) {
		this.vaultSettings = vaultSettings;
		this.environment = environment;
	}

	@Override
	public boolean isApplicable(Volume caller) {
		return caller.getImplementationType() != VolumeImpl.FUSE || !SystemUtils.IS_OS_WINDOWS || environment.useExperimentalFuse();
	}

	@Override
	public Optional<Path> chooseMountPoint(Volume caller) {
		//VaultSettings#getCustomMountPath already checks whether the saved custom mountpoint should be used
		return this.vaultSettings.getCustomMountPath().map(Paths::get);
	}

	@Override
	public boolean prepare(Volume caller, Path mountPoint) throws InvalidMountPointException {
		switch (caller.getMountPointRequirement()) {
			case PARENT_NO_MOUNT_POINT -> {
				prepareParentNoMountPoint(mountPoint);
				LOG.debug("Successfully checked custom mount point: {}", mountPoint);
				return true;
			}
			case EMPTY_MOUNT_POINT -> {
				prepareEmptyMountPoint(mountPoint);
				LOG.debug("Successfully checked custom mount point: {}", mountPoint);
				return false;
			}
			case NONE -> {
				//Requirement "NONE" doesn't make any sense here.
				//No need to prepare/verify a Mountpoint without requiring one...
				throw new InvalidMountPointException(new IllegalStateException("Illegal MountPointRequirement"));
			}
			default -> {
				//Currently the case for "UNUSED_ROOT_DIR, PARENT_OPT_MOUNT_POINT"
				throw new InvalidMountPointException(new IllegalStateException("Not implemented"));
			}
		}
	}

	void prepareParentNoMountPoint(Path mountPoint) throws InvalidMountPointException {
		//This the case on Windows when using FUSE
		//See https://github.com/billziss-gh/winfsp/issues/320
		assert SystemUtils.IS_OS_WINDOWS;

		Path hideaway = getHideaway(mountPoint);

		var mpExists = Files.exists(mountPoint);
		var hideExists = Files.exists(hideaway);

		//both resources exist (whatever type)
		//TODO: possible improvement by just deleting an _empty_ hideaway
		if (mpExists && hideExists) {
			throw new InvalidMountPointException(new FileAlreadyExistsException(hideaway.toString()));
		} else if (!mpExists && !hideExists) { //neither mountpoint nor hideaway exist
			throw new InvalidMountPointException(new NoSuchFileException(mountPoint.toString()));
		} else if (!mpExists) { //only hideaway exists

			if (!Files.isDirectory(hideaway)) {
				throw new InvalidMountPointException(new NotDirectoryException(hideaway.toString()));
			}
			LOG.info("Mountpoint {} for winfsp mount seems to be not properly cleaned up. Will be fixed on unmount.", mountPoint);
			try {
				Files.setAttribute(hideaway, "dos:hidden", true);
			} catch (IOException e) {
				throw new InvalidMountPointException(e);
			}
		} else {
			if (!Files.isDirectory(mountPoint)) {
				throw new InvalidMountPointException(new NotDirectoryException(mountPoint.toString()));
			}
			try {
				if(Files.list(mountPoint).findFirst().isPresent()) {
					throw new InvalidMountPointException(new DirectoryNotEmptyException(mountPoint.toString()));
				}
				Files.move(mountPoint, hideaway);
				Files.setAttribute(hideaway, "dos:hidden", true);
			} catch (IOException e) {
				throw new InvalidMountPointException(e);
			}
		}
	}

	private void prepareEmptyMountPoint(Path mountPoint) throws InvalidMountPointException {
		//This is the case for Windows when using Dokany and for Linux and Mac
		if (!Files.isDirectory(mountPoint)) {
			throw new InvalidMountPointException(new NotDirectoryException(mountPoint.toString()));
		}
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(mountPoint)) {
			if (ds.iterator().hasNext()) {
				throw new InvalidMountPointException(new DirectoryNotEmptyException(mountPoint.toString()));
			}
		} catch (IOException exception) {
			throw new InvalidMountPointException("IOException while checking folder content", exception);
		}
	}

	@Override
	public void cleanup(Volume caller, Path mountPoint) {
		if (VolumeImpl.FUSE == caller.getImplementationType() && MountPointRequirement.PARENT_NO_MOUNT_POINT == caller.getMountPointRequirement()) {
			Path hideaway = getHideaway(mountPoint);
			try {
				Files.move(hideaway, mountPoint);
				Files.setAttribute(mountPoint, "dos:hidden", false);
			} catch (IOException e) {
				LOG.error("Unable to clean up mountpoint {} for Winfsp mounting.");
			}
		}
	}

	//visible for testing
	Path getHideaway(Path mountPoint) {
		return mountPoint.resolveSibling(HIDEAWAY_PREFIX + mountPoint.getFileName().toString() + HIDEAWAY_SUFFIX);
	}

}

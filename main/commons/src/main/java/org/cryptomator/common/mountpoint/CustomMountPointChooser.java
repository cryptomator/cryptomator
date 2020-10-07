package org.cryptomator.common.mountpoint;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.common.vaults.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class CustomMountPointChooser implements MountPointChooser {

	public static final int PRIORITY = 0;

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
		//Disable if useExperimentalFuse is required (Win + Fuse), but set to false
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
			case PARENT_NO_MOUNT_POINT -> prepareParentNoMountPoint(mountPoint);
			case EMPTY_MOUNT_POINT -> prepareEmptyMountPoint(mountPoint);
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
		LOG.debug("Successfully checked custom mount point: {}", mountPoint);
		return false;
	}

	private void prepareParentNoMountPoint(Path mountPoint) throws InvalidMountPointException {
		//This the case on Windows when using FUSE
		//See https://github.com/billziss-gh/winfsp/issues/320
		Path parent = mountPoint.getParent();
		if (!Files.isDirectory(parent)) {
			throw new InvalidMountPointException(new NotDirectoryException(parent.toString()));
		}
		//We must use #notExists() here because notExists =/= !exists (see docs)
		if (!Files.notExists(mountPoint, LinkOption.NOFOLLOW_LINKS)) {
			//File exists OR can't be determined
			throw new InvalidMountPointException(new FileAlreadyExistsException(mountPoint.toString()));
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
	public int getPriority() {
		return PRIORITY;
	}
}

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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

class CustomMountPointChooser implements MountPointChooser {

	private static final String HIDEAWAY_PREFIX = ".~$";
	private static final String HIDEAWAY_SUFFIX = ".tmp";
	private static final String WIN_HIDDEN = "dos:hidden";
	private static final Logger LOG = LoggerFactory.getLogger(CustomMountPointChooser.class);

	private final VaultSettings vaultSettings;

	@Inject
	public CustomMountPointChooser(VaultSettings vaultSettings) {
		this.vaultSettings = vaultSettings;
	}

	@Override
	public boolean isApplicable(Volume caller) {
		return caller.getImplementationType() != VolumeImpl.WEBDAV;
	}

	@Override
	public Optional<Path> chooseMountPoint(Volume caller) {
		//VaultSettings#getCustomMountPath already checks whether the saved custom mountpoint should be used
		return this.vaultSettings.getCustomMountPath().map(Paths::get);
	}

	@Override
	public boolean prepare(Volume caller, Path mountPoint) throws InvalidMountPointException {
		return switch (caller.getMountPointRequirement()) {
			case PARENT_NO_MOUNT_POINT -> {
				prepareParentNoMountPoint(mountPoint);
				LOG.debug("Successfully checked custom mount point: {}", mountPoint);
				yield true;
			}
			case EMPTY_MOUNT_POINT -> {
				prepareEmptyMountPoint(mountPoint);
				LOG.debug("Successfully checked custom mount point: {}", mountPoint);
				yield false;
			}
			case NONE, UNUSED_ROOT_DIR, PARENT_OPT_MOUNT_POINT -> {
				throw new InvalidMountPointException(new IllegalStateException("Illegal MountPointRequirement"));
			}
		};
	}

	//This is case on Windows when using FUSE
	//See https://github.com/billziss-gh/winfsp/issues/320
	void prepareParentNoMountPoint(Path mountPoint) throws InvalidMountPointException {
		Path hideaway = getHideaway(mountPoint);
		var mpExists = Files.exists(mountPoint, LinkOption.NOFOLLOW_LINKS);
		var hideExists = Files.exists(hideaway, LinkOption.NOFOLLOW_LINKS);

		//TODO: possible improvement by just deleting an _empty_ hideaway
		if (mpExists && hideExists) { //both resources exist (whatever type)
			throw new InvalidMountPointException(new FileAlreadyExistsException(hideaway.toString()));
		} else if (!mpExists && !hideExists) { //neither mountpoint nor hideaway exist
			throw new InvalidMountPointException(new NoSuchFileException(mountPoint.toString()));
		} else if (!mpExists) { //only hideaway exists
			checkIsDirectory(hideaway);
			LOG.info("Mountpoint {} for winfsp mount seems to be not properly cleaned up. Will be fixed on unmount.", mountPoint);
			try {
				if (SystemUtils.IS_OS_WINDOWS) {
					Files.setAttribute(hideaway, WIN_HIDDEN, true, LinkOption.NOFOLLOW_LINKS);
				}
			} catch (IOException e) {
				throw new InvalidMountPointException(e);
			}
		} else { //only mountpoint exists
			try {
				checkIsDirectory(mountPoint);
				checkIsEmpty(mountPoint);

				Files.move(mountPoint, hideaway);
				if (SystemUtils.IS_OS_WINDOWS) {
					Files.setAttribute(hideaway, WIN_HIDDEN, true, LinkOption.NOFOLLOW_LINKS);
				}
			} catch (IOException e) {
				throw new InvalidMountPointException(e);
			}
		}
	}

	private void prepareEmptyMountPoint(Path mountPoint) throws InvalidMountPointException {
		//This is the case for Windows when using Dokany and for Linux and Mac
		checkIsDirectory(mountPoint);
		try {
			checkIsEmpty(mountPoint);
		} catch (IOException exception) {
			throw new InvalidMountPointException("IOException while checking folder content", exception);
		}
	}

	@Override
	public void cleanup(Volume caller, Path mountPoint) {
		if (caller.getMountPointRequirement() == MountPointRequirement.PARENT_NO_MOUNT_POINT) {
			Path hideaway = getHideaway(mountPoint);
			try {
				Files.move(hideaway, mountPoint);
				if (SystemUtils.IS_OS_WINDOWS) {
					Files.setAttribute(mountPoint, WIN_HIDDEN, false);
				}
			} catch (IOException e) {
				LOG.error("Unable to clean up mountpoint {} for Winfsp mounting.", mountPoint, e);
			}
		}
	}

	private void checkIsDirectory(Path toCheck) throws InvalidMountPointException {
		if (!Files.isDirectory(toCheck, LinkOption.NOFOLLOW_LINKS)) {
			throw new InvalidMountPointException(new NotDirectoryException(toCheck.toString()));
		}
	}

	private void checkIsEmpty(Path toCheck) throws InvalidMountPointException, IOException {
		try (var dirStream = Files.list(toCheck)) {
			if (dirStream.findFirst().isPresent()) {
				throw new InvalidMountPointException(new DirectoryNotEmptyException(toCheck.toString()));
			}
		}
	}

	//visible for testing
	Path getHideaway(Path mountPoint) {
		return mountPoint.resolveSibling(HIDEAWAY_PREFIX + mountPoint.getFileName().toString() + HIDEAWAY_SUFFIX);
	}

}

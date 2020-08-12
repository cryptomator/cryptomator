package org.cryptomator.common.mountpoint;

import org.cryptomator.common.vaults.MountPointRequirement;
import org.cryptomator.common.vaults.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOG = LoggerFactory.getLogger(CustomMountPointChooser.class);

	private final Vault vault;

	public CustomMountPointChooser(Vault vault) {
		this.vault = vault;
	}

	@Override
	public Optional<Path> chooseMountPoint() {
		//VaultSettings#getCustomMountPath already checks whether the saved custom mountpoint should be used
		return this.vault.getVaultSettings().getCustomMountPath().map(Paths::get);
	}

	@Override
	public boolean prepare(Path mountPoint) throws InvalidMountPointException {
		MountPointRequirement requirement = this.vault.getMountPointRequirement();
		//Requirement "NONE" doesn't make any sense here.
		//No need to prepare/verify a Mountpoint without requiring one...
		assert requirement != MountPointRequirement.NONE;

		//Not implemented anywhere (yet)
		assert requirement != MountPointRequirement.PARENT_OPT_MOUNT_POINT;

		if(requirement == MountPointRequirement.PARENT_NO_MOUNT_POINT) {
			//This the case on Windows when using FUSE
			//See https://github.com/billziss-gh/winfsp/issues/320

			//We must use #notExists() here because notExists =/= !exists (see docs)
			if (!Files.notExists(mountPoint, LinkOption.NOFOLLOW_LINKS)) {
				//File exists OR can't be determined
				throw wrapAsIMPE(new FileAlreadyExistsException(mountPoint.toString()));
			}
		} else if(requirement == MountPointRequirement.EMPTY_MOUNT_POINT) {
			//This is the case for Windows when using Dokany
			//and for Linux and Mac

			if (!Files.isDirectory(mountPoint)) {
				throw wrapAsIMPE(new NotDirectoryException(mountPoint.toString()));
			}
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(mountPoint)) {
				if (ds.iterator().hasNext()) {
					throw wrapAsIMPE(new DirectoryNotEmptyException(mountPoint.toString()));
				}
			} catch (IOException exception) {
				throw wrapAsIMPE(exception);
			}
		}
		LOG.debug("Successfully checked custom mount point: {}", mountPoint);
		return false;
	}

	private InvalidMountPointException wrapAsIMPE(Exception exception) {
		return new InvalidMountPointException(exception);
	}
}

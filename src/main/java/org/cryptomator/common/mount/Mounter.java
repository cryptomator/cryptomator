package org.cryptomator.common.mount;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.mount.MountFailedException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.beans.value.ObservableValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.cryptomator.integrations.mount.MountCapability.MOUNT_AS_DRIVE_LETTER;
import static org.cryptomator.integrations.mount.MountCapability.MOUNT_TO_EXISTING_DIR;
import static org.cryptomator.integrations.mount.MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH;
import static org.cryptomator.integrations.mount.MountCapability.MOUNT_WITHIN_EXISTING_PARENT;
import static org.cryptomator.integrations.mount.MountCapability.UNMOUNT_FORCED;

@Singleton
public class Mounter {

	private final Settings settings;
	private final Environment env;
	private final WindowsDriveLetters driveLetters;
	private final ObservableValue<ActualMountService> mountService;

	@Inject
	public Mounter(Settings settings, Environment env, WindowsDriveLetters driveLetters, ObservableValue<ActualMountService> mountService) {
		this.settings = settings;
		this.env = env;
		this.driveLetters = driveLetters;
		this.mountService = mountService;
	}

	public MountHandle mountAndcreateHandle(VaultSettings vaultSettings, Path cryptoFsRoot) throws IOException, MountFailedException {
		var mountService = this.mountService.getValue().service();
		var builder = mountService.forFileSystem(cryptoFsRoot);
		boolean mountWithinParent = false;

		for (var capability : mountService.capabilities()) {
			switch (capability) {
				case FILE_SYSTEM_NAME -> builder.setFileSystemName("crypto");
				case LOOPBACK_PORT -> builder.setLoopbackPort(settings.port().get()); //TODO: move port from settings to vaultsettings (see https://github.com/cryptomator/cryptomator/tree/feature/mount-setting-per-vault)
				case LOOPBACK_HOST_NAME -> env.getLoopbackAlias().ifPresent(builder::setLoopbackHostName);
				case READ_ONLY -> builder.setReadOnly(vaultSettings.usesReadOnlyMode().get());
				case MOUNT_FLAGS -> builder.setMountFlags(Objects.requireNonNullElse(vaultSettings.mountFlags().getValue(), mountService.getDefaultMountFlags()));
				case VOLUME_ID -> builder.setVolumeId(vaultSettings.getId());
				case VOLUME_NAME -> builder.setVolumeName(vaultSettings.mountName().get());
			}
		}

		//TODO: refactor logic to own method
		var userChosenMountPoint = vaultSettings.getMountPoint();
		var defaultMountPointBase = env.getMountPointsDir().orElseThrow();
		var canMountToDriveLetter = mountService.hasCapability(MOUNT_AS_DRIVE_LETTER);
		var canMountToParent = mountService.hasCapability(MOUNT_WITHIN_EXISTING_PARENT);
		var canMountToDir = mountService.hasCapability(MOUNT_TO_EXISTING_DIR);
		if (userChosenMountPoint == null) {
			if (mountService.hasCapability(MOUNT_TO_SYSTEM_CHOSEN_PATH)) {
				// no need to set a mount point
			} else if (canMountToDriveLetter) {
				builder.setMountpoint(driveLetters.getFirstDesiredAvailable().orElseThrow()); //TODO: catch exception
			} else if (canMountToParent) {
				Files.createDirectories(defaultMountPointBase);
				builder.setMountpoint(defaultMountPointBase);
			} else if (canMountToDir) {
				var mountPoint = defaultMountPointBase.resolve(vaultSettings.mountName().get());
				Files.createDirectories(mountPoint);
				builder.setMountpoint(mountPoint);
			}
		} else {
			mountWithinParent = canMountToParent && !canMountToDir;
			if(mountWithinParent) {
				// TODO: move the mount point away in case of MOUNT_WITHIN_EXISTING_PARENT
			}
			try {
				builder.setMountpoint(userChosenMountPoint);
			} catch (IllegalArgumentException e) {
				var mpIsDriveLetter = userChosenMountPoint.toString().matches("[A-Z]:\\\\");
				var configNotSupported = (!canMountToDriveLetter && mpIsDriveLetter) || (!canMountToDir && !mpIsDriveLetter) || (!canMountToParent && !mpIsDriveLetter);
				if (configNotSupported) {
					throw new MountPointNotSupportedException(e.getMessage());
				} else if (canMountToDir && !canMountToParent && !Files.exists(userChosenMountPoint)) {
					//mountpoint must exist
					throw new MountPointNotExistsException(e.getMessage());
				} else {
					throw new IllegalMountPointException(e.getMessage());
				}
				/*
				//TODO:
				if (!canMountToDir && canMountToParent && !Files.notExists(userChosenMountPoint)) {
					//parent must exist, mountpoint must not exist
				}
				 */
			}
		}

		return new MountHandle(builder.mount(), mountService.hasCapability(UNMOUNT_FORCED), mountWithinParent);
	}

	public record MountHandle(Mount mount, boolean supportsUnmountForced, boolean mountWithinParent) {

	}
}

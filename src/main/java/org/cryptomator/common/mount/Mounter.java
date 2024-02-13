package org.cryptomator.common.mount;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.integrations.mount.Mount;
import org.cryptomator.integrations.mount.MountBuilder;
import org.cryptomator.integrations.mount.MountFailedException;
import org.cryptomator.integrations.mount.MountService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javafx.beans.value.ObservableValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.cryptomator.integrations.mount.MountCapability.MOUNT_AS_DRIVE_LETTER;
import static org.cryptomator.integrations.mount.MountCapability.MOUNT_TO_EXISTING_DIR;
import static org.cryptomator.integrations.mount.MountCapability.MOUNT_TO_SYSTEM_CHOSEN_PATH;
import static org.cryptomator.integrations.mount.MountCapability.MOUNT_WITHIN_EXISTING_PARENT;
import static org.cryptomator.integrations.mount.MountCapability.UNMOUNT_FORCED;

@Singleton
public class Mounter {

	// mount providers (key) can not be used if any of the conflicting mount providers (values) are already in use
	private static final Map<String, Set<String>> CONFLICTING_MOUNT_SERVICES = Map.of(
			"org.cryptomator.frontend.fuse.mount.MacFuseMountProvider", Set.of("org.cryptomator.frontend.fuse.mount.FuseTMountProvider"),
			"org.cryptomator.frontend.fuse.mount.FuseTMountProvider", Set.of("org.cryptomator.frontend.fuse.mount.MacFuseMountProvider")
	);

	private final Environment env;
	private final Settings settings;
	private final WindowsDriveLetters driveLetters;
	private final List<MountService> mountProviders;
	private final Set<MountService> usedMountServices;
	private final ObservableValue<MountService> defaultMountService;

	@Inject
	public Mounter(Environment env, //
				   Settings settings, //
				   WindowsDriveLetters driveLetters, //
				   List<MountService> mountProviders, //
				   @Named("usedMountServices") Set<MountService> usedMountServices, //
				   ObservableValue<MountService> defaultMountService) {
		this.env = env;
		this.settings = settings;
		this.driveLetters = driveLetters;
		this.mountProviders = mountProviders;
		this.usedMountServices = usedMountServices;
		this.defaultMountService = defaultMountService;
	}

	private class SettledMounter {

		private final MountService service;
		private final MountBuilder builder;
		private final VaultSettings vaultSettings;

		public SettledMounter(MountService service, MountBuilder builder, VaultSettings vaultSettings) {
			this.service = service;
			this.builder = builder;
			this.vaultSettings = vaultSettings;
		}

		Runnable prepare() throws IOException {
			for (var capability : service.capabilities()) {
				switch (capability) {
					case FILE_SYSTEM_NAME -> builder.setFileSystemName("cryptoFs");
					case LOOPBACK_PORT -> {
						if (vaultSettings.mountService.getValue() == null) {
							builder.setLoopbackPort(settings.port.get());
						} else {
							builder.setLoopbackPort(vaultSettings.port.get());
						}
					}
					case LOOPBACK_HOST_NAME -> env.getLoopbackAlias().ifPresent(builder::setLoopbackHostName);
					case READ_ONLY -> builder.setReadOnly(vaultSettings.usesReadOnlyMode.get());
					case MOUNT_FLAGS -> {
						var mountFlags = vaultSettings.mountFlags.get();
						if (mountFlags == null || mountFlags.isBlank()) {
							builder.setMountFlags(service.getDefaultMountFlags());
						} else {
							builder.setMountFlags(mountFlags);
						}
					}
					case VOLUME_ID -> builder.setVolumeId(vaultSettings.id);
					case VOLUME_NAME -> builder.setVolumeName(vaultSettings.mountName.get());
				}
			}

			return prepareMountPoint();
		}

		private Runnable prepareMountPoint() throws IOException {
			Runnable cleanup = () -> {};
			var userChosenMountPoint = vaultSettings.mountPoint.get();
			var defaultMountPointBase = env.getMountPointsDir().orElseThrow();
			var canMountToDriveLetter = service.hasCapability(MOUNT_AS_DRIVE_LETTER);
			var canMountToParent = service.hasCapability(MOUNT_WITHIN_EXISTING_PARENT);
			var canMountToDir = service.hasCapability(MOUNT_TO_EXISTING_DIR);
			var canMountToSystem = service.hasCapability(MOUNT_TO_SYSTEM_CHOSEN_PATH);

			if (userChosenMountPoint == null) {
				if (canMountToSystem) {
					// no need to set a mount point
				} else if (canMountToDriveLetter) {
					builder.setMountpoint(driveLetters.getFirstDesiredAvailable().orElseThrow()); //TODO: catch exception and translate
				} else if (canMountToParent) {
					Files.createDirectories(defaultMountPointBase);
					builder.setMountpoint(defaultMountPointBase);
				} else if (canMountToDir) {
					var dirName = vaultSettings.mountName.get();
					//required for https://github.com/cryptomator/cryptomator/issues/3272
					if(service.getClass().getCanonicalName().equals("org.cryptomator.frontend.fuse.mount.MacFuseMountProvider")) {
						dirName = vaultSettings.id;
					}
					var mountPoint = defaultMountPointBase.resolve(dirName);
					Files.createDirectories(mountPoint);
					builder.setMountpoint(mountPoint);
				}
			} else {
				var mpIsDriveLetter = userChosenMountPoint.toString().matches("[A-Z]:\\\\");
				if (mpIsDriveLetter) {
					if (driveLetters.getOccupied().contains(userChosenMountPoint)) {
						throw new MountPointInUseException(userChosenMountPoint);
					}
				} else if (canMountToParent && !canMountToDir) {
					MountWithinParentUtil.prepareParentNoMountPoint(userChosenMountPoint);
					cleanup = () -> {
						MountWithinParentUtil.cleanup(userChosenMountPoint);
					};
				}
				try {
					builder.setMountpoint(userChosenMountPoint);
				} catch (IllegalArgumentException | UnsupportedOperationException e) {
					var configNotSupported = (!canMountToDriveLetter && mpIsDriveLetter) //mounting as driveletter, albeit not supported
							|| (!canMountToDir && !mpIsDriveLetter) //mounting to directory, albeit not supported
							|| (!canMountToParent && !mpIsDriveLetter) //
							|| (!canMountToDir && !canMountToParent && !canMountToSystem && !canMountToDriveLetter);
					if (configNotSupported) {
						throw new MountPointNotSupportedException(userChosenMountPoint, e.getMessage());
					} else if (canMountToDir && !canMountToParent && !Files.exists(userChosenMountPoint)) {
						//mountpoint must exist
						throw new MountPointNotExistingException(userChosenMountPoint, e.getMessage());
					} else {
						//TODO: add specific exception for !canMountToDir && canMountToParent && !Files.notExists(userChosenMountPoint)
						throw new IllegalMountPointException(userChosenMountPoint, e.getMessage());
					}
				}
			}
			return cleanup;
		}

	}

	public MountHandle mount(VaultSettings vaultSettings, Path cryptoFsRoot) throws IOException, MountFailedException {
		var mountService = mountProviders.stream().filter(s -> s.getClass().getName().equals(vaultSettings.mountService.getValue())).findFirst().orElse(defaultMountService.getValue());

		if (isConflictingMountService(mountService)) {
			var msg = STR."\{mountService.getClass()} unavailable due to conflict with either of \{CONFLICTING_MOUNT_SERVICES.get(mountService.getClass().getName())}";
			throw new ConflictingMountServiceException(msg);
		}

		usedMountServices.add(mountService);

		var builder = mountService.forFileSystem(cryptoFsRoot);
		var internal = new SettledMounter(mountService, builder, vaultSettings); // FIXME: no need for an inner class
		var cleanup = internal.prepare();
		return new MountHandle(builder.mount(), mountService.hasCapability(UNMOUNT_FORCED), cleanup);
	}

	public boolean isConflictingMountService(MountService service) {
		var conflictingServices = CONFLICTING_MOUNT_SERVICES.getOrDefault(service.getClass().getName(), Set.of());
		return usedMountServices.stream().map(MountService::getClass).map(Class::getName).anyMatch(conflictingServices::contains);
	}

	public record MountHandle(Mount mountObj, boolean supportsUnmountForced, Runnable specialCleanup) {

	}

}

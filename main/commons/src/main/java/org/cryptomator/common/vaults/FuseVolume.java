package org.cryptomator.common.vaults;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.fuse.mount.CommandFailedException;
import org.cryptomator.frontend.fuse.mount.EnvironmentVariables;
import org.cryptomator.frontend.fuse.mount.FuseMountFactory;
import org.cryptomator.frontend.fuse.mount.FuseNotSupportedException;
import org.cryptomator.frontend.fuse.mount.Mount;
import org.cryptomator.frontend.fuse.mount.Mounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
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

public class FuseVolume implements Volume {

	private static final Logger LOG = LoggerFactory.getLogger(FuseVolume.class);
	private static final int MAX_TMPMOUNTPOINT_CREATION_RETRIES = 10;

	private final VaultSettings vaultSettings;
	private final Environment environment;
	private final WindowsDriveLetters windowsDriveLetters;

	private Mount fuseMnt;
	private Path mountPoint;
	private boolean createdTemporaryMountPoint;

	@Inject
	public FuseVolume(VaultSettings vaultSettings, Environment environment, WindowsDriveLetters windowsDriveLetters) {
		this.vaultSettings = vaultSettings;
		this.environment = environment;
		this.windowsDriveLetters = windowsDriveLetters;
	}

	@Override
	public void mount(CryptoFileSystem fs, String mountFlags) throws IOException, FuseNotSupportedException, VolumeException {
		this.mountPoint = determineMountPoint();

		mount(fs.getPath("/"), mountFlags);
	}

	private Path determineMountPoint() throws IOException, VolumeException {
		Path mountPoint;
		Optional<String> optionalCustomMountPoint = vaultSettings.getCustomMountPath();
		//Is there a custom mountpoint?
		if (optionalCustomMountPoint.isPresent()) {
			mountPoint = Paths.get(optionalCustomMountPoint.get());
			checkProvidedMountPoint(mountPoint);
			LOG.debug("Successfully checked custom mount point: {}", this.mountPoint);
			return mountPoint;
		}
		//No custom mounpoint -> Are we on Windows?
		if (SystemUtils.IS_OS_WINDOWS) {
			//Is there a chosen Driveletter?
			if (!Strings.isNullOrEmpty(vaultSettings.winDriveLetter().get())) {
				mountPoint = Path.of(vaultSettings.winDriveLetter().get().charAt(0) + ":\\");
				return mountPoint;
			}

			mountPoint = windowsDriveLetters.getAvailableDriveLetterPath().orElseThrow(() -> {
				//TODO: Error Handling/Fallback (replace Exception with Flow to folderbased?)
				return new VolumeException("No free drive letter available.");
			});
			return mountPoint;
		}
		//Nothing worked so far or we are not using Windows - Choose and prepare a folder
		mountPoint = prepareTemporaryMountPoint();
		LOG.debug("Successfully created mount point: {}", mountPoint);
		return mountPoint;
	}

	private void checkProvidedMountPoint(Path mountPoint) throws IOException {
		//On Windows the target folder MUST NOT exist...
		//https://github.com/billziss-gh/winfsp/issues/320
		if (SystemUtils.IS_OS_WINDOWS) {
			//We must use #notExists() here because notExists =/= !exists (see docs)
			if (Files.notExists(mountPoint, LinkOption.NOFOLLOW_LINKS)) {
				//File really doesn't exist
				return;
			}
			//File exists OR can't be determined
			throw new FileAlreadyExistsException(mountPoint.toString());
		}

		//... on Mac and Linux it's the opposite
		if (!Files.isDirectory(mountPoint)) {
			throw new NotDirectoryException(mountPoint.toString());
		}
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(mountPoint)) {
			if (ds.iterator().hasNext()) {
				throw new DirectoryNotEmptyException(mountPoint.toString());
			}
		}
	}

	private Path prepareTemporaryMountPoint() throws IOException, VolumeException {
		Path mountPoint = chooseNonExistingTemporaryMountPoint();
		// https://github.com/osxfuse/osxfuse/issues/306#issuecomment-245114592:
		// In order to allow non-admin users to mount FUSE volumes in `/Volumes`,
		// starting with version 3.5.0, FUSE will create non-existent mount points automatically.
		if (SystemUtils.IS_OS_MAC && mountPoint.getParent().equals(Paths.get("/Volumes"))) {
			return mountPoint;
		}

		//WinFSP needs the parent, but the acutal Mount Point must not exist...
		if (SystemUtils.IS_OS_WINDOWS) {
			Files.createDirectories(mountPoint.getParent());
		} else {
			Files.createDirectories(mountPoint);
			this.createdTemporaryMountPoint = true;
		}
		return mountPoint;
	}

	private Path chooseNonExistingTemporaryMountPoint() throws VolumeException {
		Path parent = environment.getMountPointsDir().orElseThrow();
		String basename = vaultSettings.getId();
		for (int i = 0; i < MAX_TMPMOUNTPOINT_CREATION_RETRIES; i++) {
			Path mountPoint = parent.resolve(basename + "_" + i);
			if (Files.notExists(mountPoint)) {
				return mountPoint;
			}
		}
		LOG.error("Failed to find feasible mountpoint at {}{}{}_x. Giving up after {} attempts.", parent, File.separator, basename, MAX_TMPMOUNTPOINT_CREATION_RETRIES);
		throw new VolumeException("Did not find feasible mount point.");
	}

	private void mount(Path root, String mountFlags) throws VolumeException {
		try {
			Mounter mounter = FuseMountFactory.getMounter();
			EnvironmentVariables envVars = EnvironmentVariables.create() //
					.withFlags(splitFlags(mountFlags)).withMountPoint(mountPoint) //
					.build();
			this.fuseMnt = mounter.mount(root, envVars);
		} catch (CommandFailedException e) {
			throw new VolumeException("Unable to mount Filesystem", e);
		}
	}

	private String[] splitFlags(String str) {
		return Splitter.on(' ').splitToList(str).toArray(String[]::new);
	}

	@Override
	public void reveal() throws VolumeException {
		try {
			fuseMnt.revealInFileManager();
		} catch (CommandFailedException e) {
			LOG.debug("Revealing the vault in file manger failed: " + e.getMessage());
			throw new VolumeException(e);
		}
	}

	@Override
	public boolean supportsForcedUnmount() {
		return true;
	}

	@Override
	public synchronized void unmountForced() throws VolumeException {
		try {
			fuseMnt.unmountForced();
			fuseMnt.close();
		} catch (CommandFailedException e) {
			throw new VolumeException(e);
		}
		cleanupTemporaryMountPoint();
	}

	@Override
	public synchronized void unmount() throws VolumeException {
		try {
			fuseMnt.unmount();
			fuseMnt.close();
		} catch (CommandFailedException e) {
			throw new VolumeException(e);
		}
		cleanupTemporaryMountPoint();
	}

	private void cleanupTemporaryMountPoint() {
		if (createdTemporaryMountPoint) {
			try {
				Files.delete(mountPoint);
				LOG.debug("Successfully deleted mount point: {}", mountPoint);
			} catch (IOException e) {
				LOG.warn("Could not delete mount point: {}", e.getMessage());
			}
		}
	}

	@Override
	public boolean isSupported() {
		return FuseVolume.isSupportedStatic();
	}

	@Override
	public Optional<Path> getMountPoint() {
		return Optional.ofNullable(mountPoint);
	}

	@Override
	public MountPointRequirement getMountPointRequirement() {
		return SystemUtils.IS_OS_WINDOWS ? MountPointRequirement.PARENT_NO_MOUNT_POINT : MountPointRequirement.EMPTY_MOUNT_POINT;
	}

	public static boolean isSupportedStatic() {
		return FuseMountFactory.isFuseSupported();
	}

}

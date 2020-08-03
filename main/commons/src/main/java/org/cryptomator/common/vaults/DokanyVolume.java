package org.cryptomator.common.vaults;

import com.google.common.base.Strings;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.dokany.Mount;
import org.cryptomator.frontend.dokany.MountFactory;
import org.cryptomator.frontend.dokany.MountFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class DokanyVolume implements Volume {

	private static final Logger LOG = LoggerFactory.getLogger(DokanyVolume.class);
	private static final int MAX_TMPMOUNTPOINT_CREATION_RETRIES = 10;

	private static final String FS_TYPE_NAME = "Cryptomator File System";

	private final VaultSettings vaultSettings;
	private final MountFactory mountFactory;
	private final Environment environment;
	private final WindowsDriveLetters windowsDriveLetters;
	private Mount mount;
	private Path mountPoint;
	private boolean createdTemporaryMountPoint;

	@Inject
	public DokanyVolume(VaultSettings vaultSettings, Environment environment, ExecutorService executorService, WindowsDriveLetters windowsDriveLetters) {
		this.vaultSettings = vaultSettings;
		this.environment = environment;
		this.mountFactory = new MountFactory(executorService);
		this.windowsDriveLetters = windowsDriveLetters;
	}

	@Override
	public boolean isSupported() {
		return DokanyVolume.isSupportedStatic();
	}

	@Override
	public void mount(CryptoFileSystem fs, String mountFlags) throws VolumeException, IOException {
		this.mountPoint = determineMountPoint();
		String mountName = vaultSettings.mountName().get();
		try {
			this.mount = mountFactory.mount(fs.getPath("/"), mountPoint, mountName, FS_TYPE_NAME, mountFlags.strip());
		} catch (MountFailedException e) {
			if (vaultSettings.getCustomMountPath().isPresent()) {
				LOG.warn("Failed to mount vault into {}. Is this directory currently accessed by another process (e.g. Windows Explorer)?", mountPoint);
			}
			throw new VolumeException("Unable to mount Filesystem", e);
		}
	}

	private Path determineMountPoint() throws VolumeException, IOException {
		Optional<String> optionalCustomMountPoint = vaultSettings.getCustomMountPath();
		if (optionalCustomMountPoint.isPresent()) {
			Path customMountPoint = Paths.get(optionalCustomMountPoint.get());
			checkProvidedMountPoint(customMountPoint);
			return customMountPoint;
		}
		if (!Strings.isNullOrEmpty(vaultSettings.winDriveLetter().get())) {
			return Path.of(vaultSettings.winDriveLetter().get().charAt(0) + ":\\");
		}

		//auto assign drive letter
		Optional<Path> optionalDriveLetter = windowsDriveLetters.getAvailableDriveLetterPath();
		if (optionalDriveLetter.isPresent()) {
			return optionalDriveLetter.get();
		}

		//Nothing has worked so far -> Choose and prepare a folder
		mountPoint = prepareTemporaryMountPoint();
		LOG.debug("Successfully created mount point: {}", mountPoint);
		return mountPoint;
	}

	private void checkProvidedMountPoint(Path mountPoint) throws IOException {
		if (!Files.isDirectory(mountPoint)) {
			throw new NotDirectoryException(mountPoint.toString());
		}
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(mountPoint)) {
			if (ds.iterator().hasNext()) {
				throw new DirectoryNotEmptyException(mountPoint.toString());
			}
		}
	}

	private Path chooseNonExistingTemporaryMountPoint() throws VolumeException {
		Path parent = environment.getMountPointsDir().orElseThrow();
		String basename = vaultSettings.getId(); //FIXME
		for (int i = 0; i < MAX_TMPMOUNTPOINT_CREATION_RETRIES; i++) {
			Path mountPoint = parent.resolve(basename + "_" + i);
			if (Files.notExists(mountPoint)) {
				return mountPoint;
			}
		}
		LOG.error("Failed to find feasible mountpoint at {}{}{}_x. Giving up after {} attempts.", parent, File.separator, basename, MAX_TMPMOUNTPOINT_CREATION_RETRIES);
		throw new VolumeException("Did not find feasible mount point.");
	}

	private Path prepareTemporaryMountPoint() throws IOException, VolumeException {
		Path mountPoint = chooseNonExistingTemporaryMountPoint();

		Files.createDirectories(mountPoint);
		this.createdTemporaryMountPoint = true;

		return mountPoint;
	}

	@Override
	public void reveal() throws VolumeException {
		boolean success = mount.reveal();
		if (!success) {
			throw new VolumeException("Reveal failed.");
		}
	}

	@Override
	public void unmount() {
		mount.close();
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
	public Optional<Path> getMountPoint() {
		return Optional.ofNullable(mountPoint);
	}

	@Override
	public MountPointRequirement getMountPointRequirement() {
		return MountPointRequirement.EMPTY_MOUNT_POINT;
	}

	public static boolean isSupportedStatic() {
		return MountFactory.isApplicable();
	}
}

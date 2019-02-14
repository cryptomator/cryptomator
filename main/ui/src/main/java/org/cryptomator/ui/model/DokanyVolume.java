package org.cryptomator.ui.model;

import com.google.common.base.Strings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.dokany.Mount;
import org.cryptomator.frontend.dokany.MountFactory;
import org.cryptomator.frontend.dokany.MountFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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

	private static final String FS_TYPE_NAME = "Cryptomator File System";

	private final VaultSettings vaultSettings;
	private final MountFactory mountFactory;
	private final WindowsDriveLetters windowsDriveLetters;
	private Mount mount;

	@Inject
	public DokanyVolume(VaultSettings vaultSettings, ExecutorService executorService, WindowsDriveLetters windowsDriveLetters) {
		this.vaultSettings = vaultSettings;
		this.mountFactory = new MountFactory(executorService);
		this.windowsDriveLetters = windowsDriveLetters;
	}

	@Override
	public boolean isSupported() {
		return DokanyVolume.isSupportedStatic();
	}

	@Override
	public void mount(CryptoFileSystem fs) throws VolumeException, IOException {
		Path mountPath = getMountPoint();
		String mountName = vaultSettings.mountName().get();
		try {
			this.mount = mountFactory.mount(fs.getPath("/"), mountPath, mountName, FS_TYPE_NAME);
		} catch (MountFailedException e) {
			if (vaultSettings.getIndividualMountPath().isPresent()) {
				LOG.warn("Failed to mount vault into {}. Is this directory currently accessed by another process (e.g. Windows Explorer)?", mountPath);
			}
			throw new VolumeException("Unable to mount Filesystem", e);
		}
	}

	private Path getMountPoint() throws VolumeException, IOException {
		Optional<String> optionalCustomMountPoint = vaultSettings.getIndividualMountPath();
		if (optionalCustomMountPoint.isPresent()) {
			Path customMountPoint = Paths.get(optionalCustomMountPoint.get());
			checkProvidedMountPoint(customMountPoint);
			return customMountPoint;
		} else if (!Strings.isNullOrEmpty(vaultSettings.winDriveLetter().get())) {
			return Paths.get(vaultSettings.winDriveLetter().get().charAt(0) + ":\\");
		} else {
			//auto assign drive letter
			if (!windowsDriveLetters.getAvailableDriveLetters().isEmpty()) {
				return Paths.get(windowsDriveLetters.getAvailableDriveLetters().iterator().next() + ":\\");
			} else {
				throw new VolumeException("No free drive letter available.");
			}
		}
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
	}

	public static boolean isSupportedStatic() {
		return MountFactory.isApplicable();
	}
}

package org.cryptomator.ui.model;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.fuse.mount.CommandFailedException;
import org.cryptomator.frontend.fuse.mount.EnvironmentVariables;
import org.cryptomator.frontend.fuse.mount.FuseMountFactory;
import org.cryptomator.frontend.fuse.mount.FuseNotSupportedException;
import org.cryptomator.frontend.fuse.mount.Mount;
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

public class FuseVolume implements Volume {

	private static final Logger LOG = LoggerFactory.getLogger(FuseVolume.class);
	private static final int MAX_TMPMOUNTPOINT_CREATION_RETRIES = 10;
	private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");

	private final VaultSettings vaultSettings;
	private final Environment environment;

	private Mount fuseMnt;
	private Path mountPoint;
	private boolean createdTemporaryMountPoint;

	@Inject
	public FuseVolume(VaultSettings vaultSettings, Environment environment) {
		this.vaultSettings = vaultSettings;
		this.environment = environment;
	}

	@Override
	public void mount(CryptoFileSystem fs) throws IOException, FuseNotSupportedException, VolumeException {
		Optional<String> optionalCustomMountPoint = vaultSettings.getIndividualMountPath();
		if (optionalCustomMountPoint.isPresent()) {
			Path customMountPoint = Paths.get(optionalCustomMountPoint.get());
			checkProvidedMountPoint(customMountPoint);
			this.mountPoint = customMountPoint;
			LOG.debug("Successfully checked custom mount point: {}", mountPoint);
		} else {
			this.mountPoint = prepareTemporaryMountPoint();
			LOG.debug("Successfully created mount point: {}", mountPoint);
		}
		mount(fs.getPath("/"));
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

	private Path prepareTemporaryMountPoint() throws IOException, VolumeException {
		Path mountPoint = chooseNonExistingTemporaryMountPoint();
		// https://github.com/osxfuse/osxfuse/issues/306#issuecomment-245114592:
		// In order to allow non-admin users to mount FUSE volumes in `/Volumes`,
		// starting with version 3.5.0, FUSE will create non-existent mount points automatically.
		if (IS_MAC && mountPoint.getParent().equals(Paths.get("/Volumes"))) {
			return mountPoint;
		} else {
			Files.createDirectories(mountPoint);
			this.createdTemporaryMountPoint = true;
			return mountPoint;
		}
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
		LOG.error("Failed to find feasible mountpoint at {}/{}_x. Giving up after {} attempts.", parent, basename, MAX_TMPMOUNTPOINT_CREATION_RETRIES);
		throw new VolumeException("Did not find feasible mount point.");
	}

	private void mount(Path root) throws VolumeException {
		try {
			EnvironmentVariables envVars = EnvironmentVariables.create() //
					.withMountName(vaultSettings.mountName().getValue()) //
					.withMountPath(mountPoint) //
					.build();
			this.fuseMnt = FuseMountFactory.getMounter().mount(root, envVars);
		} catch (CommandFailedException e) {
			throw new VolumeException("Unable to mount Filesystem", e);
		}
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

	public static boolean isSupportedStatic() {
		return (SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_LINUX) && FuseMountFactory.isFuseSupported();
	}

}

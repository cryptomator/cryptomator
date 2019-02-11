package org.cryptomator.ui.model;

import org.apache.commons.lang3.SystemUtils;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FuseVolume implements Volume {

	private static final Logger LOG = LoggerFactory.getLogger(FuseVolume.class);

	// TODO: dont use fixed Strings and rather set them in some system environment variables in the cryptomator installer and load those!
	private static final String DEFAULT_MOUNTROOTPATH_MAC = System.getProperty("user.home") + "/Library/Application Support/Cryptomator";
	private static final String DEFAULT_MOUNTROOTPATH_LINUX = System.getProperty("user.home") + "/.Cryptomator";
	private static final int MAX_TMPMOUNTPOINT_CREATION_RETRIES = 10;

	private final VaultSettings vaultSettings;

	private Mount fuseMnt;
	private Path mountPoint;
	private boolean createdTemporaryMountPoint;

	@Inject
	public FuseVolume(VaultSettings vaultSettings) {
		this.vaultSettings = vaultSettings;
	}

	@Override
	public void mount(CryptoFileSystem fs) throws IOException, FuseNotSupportedException, VolumeException {
		if (vaultSettings.usesIndividualMountPath().get()) {
			Path customMountPoint = Paths.get(vaultSettings.individualMountPath().get());
			checkProvidedMountPoint(customMountPoint);
			this.mountPoint = customMountPoint;
			this.createdTemporaryMountPoint = false;
			LOG.debug("Successfully checked custom mount point: {}", mountPoint);
		} else {
			this.mountPoint = createTemporaryMountPoint();
			this.createdTemporaryMountPoint = true;
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

	private Path createTemporaryMountPoint() throws IOException {
		Path parent = Paths.get(SystemUtils.IS_OS_MAC ? DEFAULT_MOUNTROOTPATH_MAC : DEFAULT_MOUNTROOTPATH_LINUX);
		String basename = vaultSettings.getId();
		for (int i = 0; i < MAX_TMPMOUNTPOINT_CREATION_RETRIES; i++) {
			try {
				Path mountPath = parent.resolve(basename + "_" + i);
				Files.createDirectory(mountPath);
				return mountPath;
			} catch (FileAlreadyExistsException e) {
				continue;
			}
		}
		LOG.error("Failed to create mount path at {}/{}_x. Giving up after {} attempts.", parent, basename, MAX_TMPMOUNTPOINT_CREATION_RETRIES);
		throw new FileAlreadyExistsException(parent.toString() + "/" + basename);
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
		deleteTemporaryMountPoint();
	}

	@Override
	public synchronized void unmount() throws VolumeException {
		try {
			fuseMnt.unmount();
			fuseMnt.close();
		} catch (CommandFailedException e) {
			throw new VolumeException(e);
		}
		deleteTemporaryMountPoint();
	}

	private void deleteTemporaryMountPoint() {
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

package org.cryptomator.ui.model;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

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

public class FuseVolume implements Volume {

	private static final Logger LOG = LoggerFactory.getLogger(FuseVolume.class);

	/**
	 * TODO: dont use fixed Strings and rather set them in some system environment variables in the cryptomator installer and load those!
	 */
	private static final String DEFAULT_MOUNTROOTPATH_MAC = System.getProperty("user.home") + "/Library/Application Support/Cryptomator";
	private static final String DEFAULT_MOUNTROOTPATH_LINUX = System.getProperty("user.home") + "/.Cryptomator";

	private final VaultSettings vaultSettings;

	private Mount fuseMnt;
	private Path mountPath;
	private boolean extraDirCreated;

	@Inject
	public FuseVolume(VaultSettings vaultSettings) {
		this.vaultSettings = vaultSettings;
		this.extraDirCreated = false;
	}

	@Override
	public void mount(CryptoFileSystem fs) throws IOException, FuseNotSupportedException, VolumeException {
		String mountPath;
		if (vaultSettings.usesIndividualMountPath().get()) {
			//specific path given
			mountPath = vaultSettings.individualMountPath().get();
		} else {
			//choose default path & create extra directory
			mountPath = createDirIfNotExist(SystemUtils.IS_OS_MAC ? DEFAULT_MOUNTROOTPATH_MAC : DEFAULT_MOUNTROOTPATH_LINUX, vaultSettings.mountName().get());
			extraDirCreated = true;
		}
		this.mountPath = Paths.get(mountPath).toAbsolutePath();
		mount(fs.getPath("/"));
	}

	private String createDirIfNotExist(String prefix, String dirName) throws IOException {
		Path p = Paths.get(prefix, dirName + vaultSettings.getId());
		if (Files.isDirectory(p)) {
			try (DirectoryStream<Path> emptyCheck = Files.newDirectoryStream(p)) {
				if (emptyCheck.iterator().hasNext()) {
					throw new DirectoryNotEmptyException("Mount point is not empty.");
				} else {
					LOG.info("Directory already exists and is empty. Using it as mount point.");
					return p.toString();
				}
			}
		} else {
			Files.createDirectory(p);
			return p.toString();
		}
	}

	private void mount(Path root) throws VolumeException {
		try {
			EnvironmentVariables envVars = EnvironmentVariables.create()
					.withMountName(vaultSettings.mountName().getValue())
					.withMountPath(mountPath)
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
			LOG.info("Revealing the vault in file manger failed: " + e.getMessage());
			throw new VolumeException(e);
		}
	}

	@Override
	public synchronized void unmount() throws VolumeException {
		try {
			fuseMnt.close();
		} catch (CommandFailedException e) {
			throw new VolumeException(e);
		}
		cleanup();
	}

	private void cleanup() {
		if (extraDirCreated) {
			try {
				Files.delete(mountPath);
			} catch (IOException e) {
				LOG.warn("Could not delete mounting directory:" + e.getMessage());
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

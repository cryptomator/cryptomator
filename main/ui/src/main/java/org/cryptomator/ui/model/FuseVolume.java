package org.cryptomator.ui.model;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;

import org.cryptomator.frontend.fuse.mount.EnvironmentVariables;
import org.cryptomator.frontend.fuse.mount.FuseMount;
import org.cryptomator.frontend.fuse.mount.MountFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Paths;

@VaultModule.PerVault
public class FuseVolume implements Volume {

	private static final Logger LOG = LoggerFactory.getLogger(FuseVolume.class);
	private static final String DEFAULT_MOUNTROOTPATH_MAC = System.getProperty("user.home") + "/Library/Application Support/Cryptomator";
	private static final String DEFAULT_MOUNTROOTPATH_LINUX = System.getProperty("user.home") + "/.Cryptomator";

	private final FuseMount fuseMnt;
	private final VaultSettings vaultSettings;
	private final WindowsDriveLetters windowsDriveLetters;

	private CryptoFileSystem cfs;

	@Inject
	public FuseVolume(VaultSettings vaultSettings, WindowsDriveLetters windowsDriveLetters) {
		this.vaultSettings = vaultSettings;
		this.windowsDriveLetters = windowsDriveLetters;
		this.fuseMnt = MountFactory.createMountObject();
	}

	@Override
	public void prepare(CryptoFileSystem fs) {
		this.cfs = fs;
		if (!(vaultSettings.mountPath().isNotNull().get() || SystemUtils.IS_OS_WINDOWS)) {
			fuseMnt.useExtraMountDir();
		}
	}

	@Override
	public void mount() throws CommandFailedException {
		try {
			EnvironmentVariables envVars = EnvironmentVariables.create()
					.withMountName(vaultSettings.mountName().getValue() + vaultSettings.getId())
					.withMountPath(chooseMountRootPath())
					.build();
			fuseMnt.mount(cfs.getPath("/"), envVars);
		} catch (Exception e) {
			throw new CommandFailedException("Unable to mount Filesystem", e);
		}
	}

	private String chooseMountRootPath() {
		if (SystemUtils.IS_OS_WINDOWS) {
			//windows case
			if (vaultSettings.winDriveLetter().get() != null) {
				// specific drive letter selected
				return vaultSettings.winDriveLetter().getValue() + ":\\";
			} else {
				// auto assign drive letter selected
				return windowsDriveLetters.getAvailableDriveLetters().iterator().next() + ":\\";
			}
		} else {
			if (vaultSettings.mountPath().isNotNull().get()) {
				//specific path given
				vaultSettings.mountPath().getValue();
			} else {
				//choose default path
				return SystemUtils.IS_OS_MAC ? DEFAULT_MOUNTROOTPATH_MAC : DEFAULT_MOUNTROOTPATH_LINUX;
			}

		}
		return null;
	}

	@Override
	public void reveal() throws CommandFailedException {
		//fuseMnt.reveal();
	}

	@Override
	public synchronized void unmount() throws CommandFailedException {
		if (cfs.getStats().pollBytesRead() == 0 && cfs.getStats().pollBytesWritten() == 0) {
			unmountRaw();
		} else {
			throw new CommandFailedException("Pending read or write operations.");
		}
	}

	@Override
	public synchronized void unmountForced() throws CommandFailedException {
		this.unmountRaw();
	}

	private synchronized void unmountRaw() throws CommandFailedException {
		try {
			fuseMnt.unmount();
		} catch (org.cryptomator.frontend.fuse.mount.CommandFailedException e) {
			throw new CommandFailedException(e);
		}
	}

	@Override
	public void stop() {
		try {
			fuseMnt.cleanUp();
		} catch (org.cryptomator.frontend.fuse.mount.CommandFailedException e) {
			LOG.warn(e.getMessage());
		}
	}

	@Override
	public String getMountUri() {
		return Paths.get(fuseMnt.getMountPath()).toUri().toString();
	}

	/**
	 * TODO: chang this to a real implementation
	 *
	 * @return
	 */
	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	public boolean supportsForcedUnmount() {
		return true;
	}

}

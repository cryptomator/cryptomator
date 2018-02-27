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
public class FuseNioAdapter implements NioAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(FuseNioAdapter.class);

	private final FuseMount fuseMnt;
	private final VaultSettings vaultSettings;
	private final WindowsDriveLetters windowsDriveLetters;

	private CryptoFileSystem cfs;

	@Inject
	public FuseNioAdapter(VaultSettings vaultSettings, WindowsDriveLetters windowsDriveLetters) {
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
					.withMountPath(
							SystemUtils.IS_OS_WINDOWS? computeWinDriveLetter() : vaultSettings.mountPath().getValue())
					.build();
			fuseMnt.mount(cfs.getPath("/"), envVars);
		} catch (Exception e) {
			throw new CommandFailedException("Unable to mount Filesystem", e);
		}
	}

	private String computeWinDriveLetter(){
		if(vaultSettings.winDriveLetter().get() != null){
			return vaultSettings.winDriveLetter().getValue()+":\\";
		}
		else{
			return windowsDriveLetters.getAvailableDriveLetters().iterator().next()+":\\";
		}
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

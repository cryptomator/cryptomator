package org.cryptomator.ui.model;

import com.google.common.base.Strings;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.dokany.Mount;
import org.cryptomator.frontend.dokany.MountFactory;
import org.cryptomator.frontend.dokany.MountFailedException;

import javax.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;

public class DokanyVolume implements Volume {

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
	public void mount(CryptoFileSystem fs) throws VolumeException {
		Path mountPath = Paths.get(getMountPathString());
		String mountName = vaultSettings.mountName().get();
		try {
			this.mount = mountFactory.mount(fs.getPath("/"), mountPath, mountName, FS_TYPE_NAME);
		} catch (MountFailedException e) {
			throw new VolumeException("Unable to mount Filesystem", e);
		}
	}

	private String getMountPathString() throws VolumeException {
		if (vaultSettings.usesIndividualMountPath().get()) {
			return vaultSettings.individualMountPath().get();
		} else if (!Strings.isNullOrEmpty(vaultSettings.winDriveLetter().get())) {
			return vaultSettings.winDriveLetter().get().charAt(0) + ":\\";
		} else {
			//auto assign drive letter
			if (!windowsDriveLetters.getAvailableDriveLetters().isEmpty()) {
				return windowsDriveLetters.getAvailableDriveLetters().iterator().next() + ":\\";
			} else {
				throw new VolumeException("No free drive letter available.");
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

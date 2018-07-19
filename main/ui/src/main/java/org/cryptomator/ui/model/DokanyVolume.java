package org.cryptomator.ui.model;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.dokany.Mount;
import org.cryptomator.frontend.dokany.MountFactory;

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
		return this.isSupportedStatic();
	}

	//TODO: Drive letter 'A' as mount point is invalid in dokany. maybe we should do already here something against it
	@Override
	public void mount(CryptoFileSystem fs) throws VolumeException {
		char driveLetter;
		if (!vaultSettings.winDriveLetter().getValueSafe().equals("")) {
			driveLetter = vaultSettings.winDriveLetter().get().charAt(0);
		} else {
			//auto assign drive letter
			//TODO: can we assume the we have at least one free drive letter?

			//this is a temporary fix for 'A' being an invalid drive letter
			if (!windowsDriveLetters.getAvailableDriveLetters().isEmpty()) {
				Iterator<Character> winDriveLetterIt = windowsDriveLetters.getAvailableDriveLetters().iterator();
				do {
					driveLetter = winDriveLetterIt.next();
				} while (winDriveLetterIt.hasNext() && driveLetter == 65);
//			if (!windowsDriveLetters.getAvailableDriveLetters().isEmpty()) {
//				driveLetter = windowsDriveLetters.getAvailableDriveLetters().iterator().next();
			} else {
				throw new VolumeException("No free drive letter available.");
			}
		}
		String mountName = vaultSettings.mountName().get();
		this.mount = mountFactory.mount(fs.getPath("/"), driveLetter, mountName, FS_TYPE_NAME);
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

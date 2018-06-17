package org.cryptomator.ui.model;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.dokany.Mount;
import org.cryptomator.frontend.dokany.MountFactory;

@VaultModule.PerVault
public class DokanyVolume implements Volume {

	private static final String FS_TYPE_NAME = "Cryptomator File System";

	private final VaultSettings vaultSettings;
	private final MountFactory mountFactory;
	private Mount mount;

	@Inject
	public DokanyVolume(VaultSettings vaultSettings, ExecutorService executorService) {
		this.vaultSettings = vaultSettings;
		this.mountFactory = new MountFactory(executorService);
	}


	@Override
	public boolean isSupported() {
		return MountFactory.isApplicable();
	}

	@Override
	public void mount(CryptoFileSystem fs) {
		char driveLetter = vaultSettings.winDriveLetter().get().charAt(0);
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
}

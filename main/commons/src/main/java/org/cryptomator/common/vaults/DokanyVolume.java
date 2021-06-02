package org.cryptomator.common.vaults;

import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.mountpoint.MountPointChooser;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.dokany.DokanyMountFailedException;
import org.cryptomator.frontend.dokany.Mount;
import org.cryptomator.frontend.dokany.MountFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.function.Consumer;

public class DokanyVolume extends AbstractVolume {

	private static final Logger LOG = LoggerFactory.getLogger(DokanyVolume.class);

	private static final String FS_TYPE_NAME = "CryptomatorFS";

	private final VaultSettings vaultSettings;

	private Mount mount;

	@Inject
	public DokanyVolume(VaultSettings vaultSettings, @Named("orderedMountPointChoosers") Iterable<MountPointChooser> choosers) {
		super(choosers);
		this.vaultSettings = vaultSettings;
	}

	@Override
	public VolumeImpl getImplementationType() {
		return VolumeImpl.DOKANY;
	}

	@Override
	public void mount(CryptoFileSystem fs, String mountFlags, Consumer<Throwable> onExitAction) throws InvalidMountPointException, VolumeException {
		this.mountPoint = determineMountPoint();
		try {
			this.mount = MountFactory.mount(fs.getPath("/"), mountPoint, vaultSettings.mountName().get(), FS_TYPE_NAME, mountFlags.strip(), onExitAction);
		} catch (DokanyMountFailedException e) {
			if (vaultSettings.getCustomMountPath().isPresent()) {
				LOG.warn("Failed to mount vault into {}. Is this directory currently accessed by another process (e.g. Windows Explorer)?", mountPoint);
			}
			throw new VolumeException("Unable to mount Filesystem", e);
		}
	}

	@Override
	public void reveal(Revealer revealer) throws VolumeException {
		try {
			mount.reveal(revealer::reveal);
		} catch (Exception e) {
			throw new VolumeException(e);
		}
	}

	@Override
	public void unmount() throws VolumeException {
		try {
			mount.unmount();
		} catch (IllegalStateException e) {
			throw new VolumeException("Unmount Failed.", e);
		}
		cleanupMountPoint();
	}

	@Override
	public void unmountForced() {
		mount.unmountForced();
		cleanupMountPoint();
	}

	@Override
	public boolean supportsForcedUnmount() {
		return true;
	}

	@Override
	public boolean isSupported() {
		return DokanyVolume.isSupportedStatic();
	}

	@Override
	public MountPointRequirement getMountPointRequirement() {
		return MountPointRequirement.EMPTY_MOUNT_POINT;
	}

	public static boolean isSupportedStatic() {
		return MountFactory.isApplicable();
	}
}

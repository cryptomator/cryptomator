package org.cryptomator.common.vaults;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.mountpoint.MountPointChooser;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.cryptofs.CryptoFileSystem;
import org.cryptomator.frontend.dokany.Mount;
import org.cryptomator.frontend.dokany.MountFactory;
import org.cryptomator.frontend.dokany.MountFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class DokanyVolume implements Volume {

	private static final Logger LOG = LoggerFactory.getLogger(DokanyVolume.class);
	private static final int MAX_TMPMOUNTPOINT_CREATION_RETRIES = 10;

	private static final String FS_TYPE_NAME = "Cryptomator File System";

	private final VaultSettings vaultSettings;
	private final MountFactory mountFactory;

	private final Set<MountPointChooser> choosers;

	private Mount mount;
	private Path mountPoint;

	//Cleanup
	private boolean cleanupRequired;
	private MountPointChooser usedChooser;

	@Inject
	public DokanyVolume(VaultSettings vaultSettings, ExecutorService executorService, @Named("orderedValidMountPointChoosers") Set<MountPointChooser> choosers) {
		this.vaultSettings = vaultSettings;
		this.mountFactory = new MountFactory(executorService);
		this.choosers = choosers;
	}

	@Override
	public boolean isSupported() {
		return DokanyVolume.isSupportedStatic();
	}

	@Override
	public void mount(CryptoFileSystem fs, String mountFlags) throws InvalidMountPointException, VolumeException {
		this.mountPoint = determineMountPoint();
		String mountName = vaultSettings.mountName().get();
		try {
			this.mount = mountFactory.mount(fs.getPath("/"), mountPoint, mountName, FS_TYPE_NAME, mountFlags.strip());
		} catch (MountFailedException e) {
			if (vaultSettings.getCustomMountPath().isPresent()) {
				LOG.warn("Failed to mount vault into {}. Is this directory currently accessed by another process (e.g. Windows Explorer)?", mountPoint);
			}
			throw new VolumeException("Unable to mount Filesystem", e);
		}
	}

	private Path determineMountPoint() throws InvalidMountPointException {
		for (MountPointChooser chooser : this.choosers) {
			Optional<Path> chosenPath = chooser.chooseMountPoint();
			if (chosenPath.isEmpty()) {
				//Chooser was applicable, but couldn't find a feasible mountpoint
				continue;
			}
			this.cleanupRequired = chooser.prepare(chosenPath.get()); //Fail entirely if an Exception occurs
			this.usedChooser = chooser;
			return chosenPath.get();
		}
		String tried = Joiner.on(", ").join(this.choosers.stream()
				.map((mpc) -> mpc.getClass().getTypeName())
				.collect(ImmutableSet.toImmutableSet()));
		throw new InvalidMountPointException(String.format("No feasible MountPoint found! Tried %s", tried));
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
		cleanupMountPoint();
	}

	private void cleanupMountPoint() {
		if (this.cleanupRequired) {
			this.usedChooser.cleanup(this.mountPoint);
		}
	}

	@Override
	public Optional<Path> getMountPoint() {
		return Optional.ofNullable(mountPoint);
	}

	@Override
	public MountPointRequirement getMountPointRequirement() {
		return MountPointRequirement.EMPTY_MOUNT_POINT;
	}

	public static boolean isSupportedStatic() {
		return MountFactory.isApplicable();
	}
}

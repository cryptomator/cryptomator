package org.cryptomator.common.vaults;

import com.google.common.collect.Iterables;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.mountpoint.MountPointChooser;

import java.nio.file.Path;
import java.util.Optional;

public abstract class AbstractVolume implements Volume {

	private final Iterable<MountPointChooser> choosers;

	protected Path mountPoint;
	private boolean cleanupRequired;
	private MountPointChooser usedChooser;

	public AbstractVolume(Iterable<MountPointChooser> choosers) {
		this.choosers = choosers;
	}

	protected Path determineMountPoint() throws InvalidMountPointException {
		for (var chooser : Iterables.filter(choosers, c -> c.isApplicable(this))) {
			Optional<Path> chosenPath = chooser.chooseMountPoint(this);
			if (chosenPath.isEmpty()) { // chooser couldn't find a feasible mountpoint
				continue;
			}
			this.cleanupRequired = chooser.prepare(this, chosenPath.get());
			this.usedChooser = chooser;
			return chosenPath.get();
		}
		throw new InvalidMountPointException("No feasible MountPoint found!");
	}

	protected void cleanupMountPoint() {
		if (this.cleanupRequired) {
			this.usedChooser.cleanup(this, this.mountPoint);
		}
	}

	@Override
	public Optional<Path> getMountPoint() {
		return Optional.ofNullable(mountPoint);
	}
}

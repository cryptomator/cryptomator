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
		var applicableChoosers = Iterables.filter(choosers, c -> c.isApplicable(this));
		for (var chooser : applicableChoosers) {
			Optional<Path> chosenPath = chooser.chooseMountPoint(this);
			if (chosenPath.isEmpty()) { // chooser couldn't find a feasible mountpoint
				continue;
			}
			this.cleanupRequired = chooser.prepare(this, chosenPath.get());
			this.usedChooser = chooser;
			return chosenPath.get();
		}
		throw new InvalidMountPointException(String.format("No feasible MountPoint found by choosers: %s", applicableChoosers));
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

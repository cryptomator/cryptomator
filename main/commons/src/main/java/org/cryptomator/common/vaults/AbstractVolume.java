package org.cryptomator.common.vaults;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.mountpoint.MountPointChooser;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractVolume implements Volume {

	private final Set<MountPointChooser> choosers;

	protected Path mountPoint;

	//Cleanup
	private boolean cleanupRequired;
	private MountPointChooser usedChooser;

	public AbstractVolume(Set<MountPointChooser> choosers) {
		this.choosers = choosers;
	}

	protected Path determineMountPoint() throws InvalidMountPointException {
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
		String tried = Joiner.on(", ").join(this.choosers.stream().map((mpc) -> mpc.getClass().getTypeName()).collect(ImmutableSet.toImmutableSet()));
		throw new InvalidMountPointException(String.format("No feasible MountPoint found! Tried %s", tried));
	}

	protected void cleanupMountPoint() {
		if (this.cleanupRequired) {
			this.usedChooser.cleanup(this.mountPoint);
		}
	}

	@Override
	public Optional<Path> getMountPoint() {
		return Optional.ofNullable(mountPoint);
	}
}

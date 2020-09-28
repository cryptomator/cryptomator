package org.cryptomator.common.vaults;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.mountpoint.MountPointChooser;

import java.nio.file.Path;
import java.util.Optional;
import java.util.SortedSet;

public abstract class AbstractVolume implements Volume {

	private final SortedSet<MountPointChooser> choosers;

	protected Path mountPoint;

	//Cleanup
	private boolean cleanupRequired;
	private MountPointChooser usedChooser;

	public AbstractVolume(SortedSet<MountPointChooser> choosers) {
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
		//SortedSet#stream() should return a sorted stream (that's what it's docs and the docs of #spliterator() say, even if they are not 100% clear for me.)
		//We want to keep that order, that's why we use ImmutableSet#toImmutableSet() to collect (even if it doesn't implement SortedSet, it's docs promise use encounter ordering.)
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

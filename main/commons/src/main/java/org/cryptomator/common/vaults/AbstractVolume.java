package org.cryptomator.common.vaults;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import org.cryptomator.common.mountpoint.InvalidMountPointException;
import org.cryptomator.common.mountpoint.MountPointChooser;

import java.nio.file.Path;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

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
		SortedSet<MountPointChooser> checkedChoosers = new TreeSet<>(); //Natural order
		for (MountPointChooser chooser : this.choosers) {
			if (!chooser.isApplicable(this)) {
				continue;
			}

			Optional<Path> chosenPath = chooser.chooseMountPoint(this);
			checkedChoosers.add(chooser); //Consider a chooser checked if it's #chooseMountPoint() method was called
			if (chosenPath.isEmpty()) {
				//Chooser was applicable, but couldn't find a feasible mountpoint
				continue;
			}
			this.cleanupRequired = chooser.prepare(this, chosenPath.get()); //Fail entirely if an Exception occurs
			this.usedChooser = chooser;
			return chosenPath.get();
		}
		//SortedSet#stream() should return a sorted stream (that's what it's docs and the docs of #spliterator() say, even if they are not 100% clear for me.)
		//We want to keep that order, that's why we use ImmutableSet#toImmutableSet() to collect (even if it doesn't implement SortedSet, it's docs promise use encounter ordering.)
		String checked = Joiner.on(", ").join(checkedChoosers.stream().map((mpc) -> mpc.getClass().getTypeName()).collect(ImmutableSet.toImmutableSet()));
		throw new InvalidMountPointException(String.format("No feasible MountPoint found! Checked %s", checked.isBlank() ? "<No applicable MPC>" : checked));
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

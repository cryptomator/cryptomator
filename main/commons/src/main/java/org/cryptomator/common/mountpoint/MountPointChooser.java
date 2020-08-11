package org.cryptomator.common.mountpoint;

import dagger.MapKey;

import java.nio.file.Path;
import java.util.Optional;

public interface MountPointChooser {

	default boolean isApplicable() {
		return true; //Usually most of the choosers should be applicable
	}

	Optional<Path> chooseMountPoint();

	default boolean prepare(Path mountPoint) throws InvalidMountPointException {
		return false; //NO-OP
	}

	default void cleanup(Path mountPoint) {
		//NO-OP
	}

	enum Phase {

		CUSTOM_MOUNTPOINT(0),

		CUSTOM_DRIVELETTER(1),

		AVAILABLE_DRIVELETTER(2),

		TEMPORARY_MOUNTPOINT(3);

		private final int timing;

		Phase(int timing) {
			this.timing = timing;
		}

		public int getTiming() {
			return timing;
		}
	}

	@MapKey
	@interface PhaseKey {

		Phase value();
	}
}

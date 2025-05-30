package org.cryptomator.common.mount;

import java.nio.file.Path;

/**
 * Indicates that validation or preparation of a mountpoint failed due to a configuration error or an invalid system state.<br>
 * Instances of this exception are usually caught and displayed to the user in an appropriate fashion, e.g. by {@link org.cryptomator.ui.unlock.UnlockInvalidMountPointController UnlockInvalidMountPointController.}
 */
public class IllegalMountPointException extends IllegalArgumentException {

	private final Path mountpoint;

	public IllegalMountPointException(Path mountpoint) {
		this(mountpoint, "The provided mountpoint has a problem: " + mountpoint.toString());
	}

	public IllegalMountPointException(Path mountpoint, String msg) {
		super(msg);
		this.mountpoint = mountpoint;
	}

	public Path getMountpoint() {
		return mountpoint;
	}
}
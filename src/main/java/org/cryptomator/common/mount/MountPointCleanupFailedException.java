package org.cryptomator.common.mount;

import java.nio.file.Path;

public class MountPointCleanupFailedException extends IllegalMountPointException {

	public MountPointCleanupFailedException(Path path) {
		super(path, "Mountpoint could not be cleared: " + path.toString());
	}
}
package org.cryptomator.common.mount;

import java.nio.file.Path;

public class MountPointCouldNotBeClearedException extends IllegalMountPointException {

	public MountPointCouldNotBeClearedException(Path path) {
		super(path, "Mountpoint could not be cleared: " + path.toString());
	}
}
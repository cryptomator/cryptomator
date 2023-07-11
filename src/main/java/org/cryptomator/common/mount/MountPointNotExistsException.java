package org.cryptomator.common.mount;

import java.nio.file.Path;

public class MountPointNotExistsException extends IllegalMountPointException {

	public MountPointNotExistsException(Path path, String msg) {
		super(path, msg);
	}

	public MountPointNotExistsException(Path path) {
		super(path, "Mountpoint does not exist: " + path);
	}
}

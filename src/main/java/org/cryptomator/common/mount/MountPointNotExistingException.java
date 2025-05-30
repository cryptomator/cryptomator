package org.cryptomator.common.mount;

import java.nio.file.Path;

public class MountPointNotExistingException extends IllegalMountPointException {

	public MountPointNotExistingException(Path path, String msg) {
		super(path, msg);
	}

	public MountPointNotExistingException(Path path) {
		super(path, "Mountpoint does not exist: " + path);
	}
}

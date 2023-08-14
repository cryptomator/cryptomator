package org.cryptomator.common.mount;

import java.nio.file.Path;

public class MountPointNotSupportedException extends IllegalMountPointException {

	public MountPointNotSupportedException(Path path, String msg) {
		super(path, msg);
	}
}

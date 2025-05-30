package org.cryptomator.common.mount;

import java.nio.file.Path;

public class MountPointInUseException extends IllegalMountPointException {

	public MountPointInUseException(Path path) {
		super(path);
	}
}

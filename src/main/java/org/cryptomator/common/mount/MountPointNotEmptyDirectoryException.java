package org.cryptomator.common.mount;

import java.nio.file.Path;

public class MountPointNotEmptyDirectoryException extends IllegalMountPointException {

	public MountPointNotEmptyDirectoryException(Path path, String msg) {
		super(path, msg);
	}
}
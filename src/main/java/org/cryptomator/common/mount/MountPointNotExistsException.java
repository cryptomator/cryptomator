package org.cryptomator.common.mount;

public class MountPointNotExistsException extends IllegalMountPointException {

	public MountPointNotExistsException(String msg) {
		super(msg);
	}
}

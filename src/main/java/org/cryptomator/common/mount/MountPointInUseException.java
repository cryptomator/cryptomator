package org.cryptomator.common.mount;

public class MountPointInUseException extends IllegalMountPointException {

	public MountPointInUseException(String msg) {
		super(msg);
	}
}

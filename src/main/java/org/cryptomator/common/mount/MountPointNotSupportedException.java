package org.cryptomator.common.mount;

public class MountPointNotSupportedException extends IllegalMountPointException {

	public MountPointNotSupportedException(String msg) {
		super(msg);
	}
}

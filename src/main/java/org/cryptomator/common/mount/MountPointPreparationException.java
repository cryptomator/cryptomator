package org.cryptomator.common.mount;

public class MountPointPreparationException extends RuntimeException {

	public MountPointPreparationException(String msg) {
		super(msg);
	}

	public MountPointPreparationException(Throwable cause) {
		super(cause);
	}
}

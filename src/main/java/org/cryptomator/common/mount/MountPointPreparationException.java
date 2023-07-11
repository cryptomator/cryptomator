package org.cryptomator.common.mount;

/**
 * Used for wrapping exceptions which occur while preparing the mountpoint.<br>
 * Instances of this exception are usually treated as generic error.
 *
 * @see IllegalMountPointException IllegalMountPointException for indicating configuration errors.
 */
public class MountPointPreparationException extends RuntimeException {

	public MountPointPreparationException(Throwable cause) {
		super(cause);
	}

	public MountPointPreparationException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
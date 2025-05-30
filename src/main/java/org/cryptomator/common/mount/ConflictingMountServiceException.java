package org.cryptomator.common.mount;

import org.cryptomator.integrations.mount.MountFailedException;

/**
 * Thrown by {@link Mounter} to indicate that the selected mount service can not be used
 * due to incompatibilities with a different mount service that is already in use.
 */
public class ConflictingMountServiceException extends MountFailedException {

	public ConflictingMountServiceException(String msg) {
		super(msg);
	}
}

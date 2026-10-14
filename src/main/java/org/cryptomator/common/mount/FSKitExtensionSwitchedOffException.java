package org.cryptomator.common.mount;

import org.cryptomator.integrations.mount.MountFailedException;

/**
 * Thrown by {@link Mounter} to indicate that mounting failed because the FSKit extension is switched off.
 */
public class FSKitExtensionSwitchedOffException extends MountFailedException {

	public FSKitExtensionSwitchedOffException(MountFailedException cause) {
		super(cause.getMessage(), cause);
	}
}

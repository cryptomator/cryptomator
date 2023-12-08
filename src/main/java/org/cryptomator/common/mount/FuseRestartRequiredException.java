package org.cryptomator.common.mount;

import org.cryptomator.integrations.mount.MountFailedException;

public class FuseRestartRequiredException extends MountFailedException {

	public FuseRestartRequiredException(String msg) {
		super(msg);
	}
}

package org.cryptomator.common.mount;

import java.nio.file.Path;

public class HideawayAlreadyExistsException extends IllegalMountPointException {

	public HideawayAlreadyExistsException(Path path, String msg) {
		super(path, msg);
	}
}
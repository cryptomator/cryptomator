package org.cryptomator.common.mount;

import java.nio.file.Path;

public class HideawayAlreadyExistsException extends IllegalMountPointException {

	private final Path hideaway;

	public HideawayAlreadyExistsException(Path path, Path hideaway, String msg) {
		super(path, msg);
		this.hideaway = hideaway;
	}

	public Path getHideaway() {
		return hideaway;
	}
}
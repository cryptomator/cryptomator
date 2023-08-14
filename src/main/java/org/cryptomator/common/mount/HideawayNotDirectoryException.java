package org.cryptomator.common.mount;

import java.nio.file.Path;

public class HideawayNotDirectoryException extends IllegalMountPointException {

	private final Path hideaway;

	public HideawayNotDirectoryException(Path path, Path hideaway) {
		super(path, "Existing hideaway (" + hideaway.toString() + ") for mountpoint is not a directory: " + path.toString());
		this.hideaway = hideaway;
	}

	public Path getHideaway() {
		return hideaway;
	}
}
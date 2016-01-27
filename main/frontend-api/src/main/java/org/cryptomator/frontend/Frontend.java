package org.cryptomator.frontend;

import java.util.Map;
import java.util.Optional;

public interface Frontend extends AutoCloseable {

	public enum MountParam {
		MOUNT_NAME, WIN_DRIVE_LETTER
	}

	void mount(Map<MountParam, Optional<String>> map) throws CommandFailedException;

	void unmount() throws CommandFailedException;

	void reveal() throws CommandFailedException;

}

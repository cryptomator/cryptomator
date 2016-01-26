package org.cryptomator.frontend;

import java.util.Map;
import java.util.Optional;

public interface Frontend extends AutoCloseable {

	public enum MountParam {
		MOUNT_NAME, WIN_DRIVE_LETTER
	}

	boolean mount(Map<MountParam, Optional<String>> map);

	void unmount();

	void reveal();

}

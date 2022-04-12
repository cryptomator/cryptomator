package org.cryptomator.launcher;

import java.nio.file.Path;
import java.util.Collection;

public record AppLaunchEvent(AppLaunchEvent.EventType type, Collection<Path> pathsToOpen) {

	public enum EventType {
		REVEAL_APP,
		OPEN_FILE
	}

}

package org.cryptomator.ui.launcher;

import java.nio.file.Path;
import java.util.Collection;

public class AppLaunchEvent {

	private final EventType type;
	private final Collection<Path> pathsToOpen;

	public enum EventType {
		REVEAL_APP,
		OPEN_FILE
	}

	public AppLaunchEvent(EventType type, Collection<Path> pathsToOpen) {
		this.type = type;
		this.pathsToOpen = pathsToOpen;
	}

	public EventType getType() {
		return type;
	}

	public Collection<Path> getPathsToOpen() {
		return pathsToOpen;
	}
}

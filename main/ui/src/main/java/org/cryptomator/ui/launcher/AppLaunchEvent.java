package org.cryptomator.ui.launcher;

import java.nio.file.Path;
import java.util.stream.Stream;

public class AppLaunchEvent {

	private final Stream<Path> pathsToOpen;
	private final EventType type;

	public enum EventType {REVEAL_APP, OPEN_FILE}

	public AppLaunchEvent(EventType type, Stream<Path> pathsToOpen) {
		this.type = type;
		this.pathsToOpen = pathsToOpen;
	}

	public EventType getType() {
		return type;
	}

	public Stream<Path> getPathsToOpen() {
		return pathsToOpen;
	}
}

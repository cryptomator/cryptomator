package org.cryptomator.launcher;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public record AppLaunchEvent(AppLaunchEvent.EventType type, Collection<Path> pathsToOpen, URI uri) {

	public enum EventType {
		REVEAL_APP,
		OPEN_FILE,
		OPEN_URI
	}

	static AppLaunchEvent revealApp() {
		return new AppLaunchEvent(EventType.REVEAL_APP, List.of(), null);
	}

	static AppLaunchEvent openFiles(Collection<Path> pathsToOpen) {
		return new AppLaunchEvent(EventType.OPEN_FILE, pathsToOpen, null);
	}

	static AppLaunchEvent openUri(URI uri) {
		return new AppLaunchEvent(EventType.OPEN_URI, List.of(), uri);
	}

}

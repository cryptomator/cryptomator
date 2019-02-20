package org.cryptomator.ui.model;

import java.nio.file.Path;
import java.util.stream.Stream;

public class AppLaunchEvent {

	private final Stream<Path> pathsToOpen;

	public AppLaunchEvent(Stream<Path> pathsToOpen) {this.pathsToOpen = pathsToOpen;}

	public Stream<Path> getPathsToOpen() {
		return pathsToOpen;
	}
}

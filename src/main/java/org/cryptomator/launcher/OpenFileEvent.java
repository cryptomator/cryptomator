package org.cryptomator.launcher;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Requests that the given paths (e.g. {@code .cryptomator} vault files) are opened.
 *
 * @param pathsToOpen the paths to open
 */
public record OpenFileEvent(Collection<Path> pathsToOpen) implements AppLaunchEvent {

}

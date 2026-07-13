package org.cryptomator.launcher;

/**
 * Requests that the already-running application instance reveals itself (brings its main window to the front).
 */
public record RevealRunningEvent() implements AppLaunchEvent {

}

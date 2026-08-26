package org.cryptomator.launcher;

/**
 * An event triggering an action in the running application instance.
 * <p>
 * Produced by the launch-argument handling (see {@link LaunchArgsParser} and the {@code *RequestHandler}s) and consumed
 * by the UI's {@code AppLaunchEventHandler}. Each permitted subtype represents one supported action:
 * <ul>
 *     <li>{@link RevealRunningEvent} - reveal the already-running app,</li>
 *     <li>{@link OpenFileEvent} - open one or more paths,</li>
 *     <li>{@link OpenHubVaultEvent} - open a Hub vault from a deeplink.</li>
 * </ul>
 */
public sealed interface AppLaunchEvent permits RevealRunningEvent, OpenFileEvent, OpenHubVaultEvent {

}

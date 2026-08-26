package org.cryptomator.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;

@Singleton
public class URIOpenRequestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(URIOpenRequestHandler.class);

	/**
	 * The registered deeplink parsers, tried in order. Each returns a matching event, an empty optional if the URI is
	 * not its concern, or throws {@link IllegalArgumentException} if the URI is its concern but malformed.
	 */
	private static final List<Function<URI, Optional<? extends AppLaunchEvent>>> DEEPLINK_PARSERS = List.of( //
			OpenHubVaultEvent::tryParse //
	);

	private final BlockingQueue<AppLaunchEvent> launchEventQueue;

	@Inject
	public URIOpenRequestHandler(@Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue) {
		this.launchEventQueue = launchEventQueue;
	}

	public void handleLaunchArgs(URI uri) {
		AppLaunchEvent launchEvent = toLaunchEvent(uri);
		if (!launchEventQueue.offer(launchEvent)) {
			LOG.warn("Could not enqueue application launch event {}.", launchEvent);
		}
	}

	private AppLaunchEvent toLaunchEvent(URI uri) {
		try {
			for (var parser : DEEPLINK_PARSERS) {
				var event = parser.apply(uri);
				if (event.isPresent()) {
					return event.get();
				}
			}
		} catch (IllegalArgumentException e) {
			LOG.warn("Received malformed deeplink {}: {}. Revealing running app instead.", uri, e.getMessage());
			return new RevealRunningEvent();
		}
		LOG.warn("Received unsupported deeplink {}, revealing running app instead.", uri);
		return new RevealRunningEvent();
	}

}

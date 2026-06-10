package org.cryptomator.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.util.concurrent.BlockingQueue;

@Singleton
public class URIOpenRequestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(URIOpenRequestHandler.class);

	private final BlockingQueue<AppLaunchEvent> launchEventQueue;

	@Inject
	public URIOpenRequestHandler(@Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue) {
		this.launchEventQueue = launchEventQueue;
	}

	public void handleLaunchArgs(URI uri) {
		AppLaunchEvent launchEvent = AppLaunchEvent.openUri(uri);
		if (!launchEventQueue.offer(launchEvent)) {
			LOG.warn("Could not enqueue application launch event {}.", launchEvent);
		}
	}

}

package org.cryptomator.launcher;

import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.BlockingQueue;

@Singleton
public class NoopRequestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(NoopRequestHandler.class);

    private final BlockingQueue<AppLaunchEvent> launchEventQueue;

	@Inject
	public NoopRequestHandler(@Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue) {
		this.launchEventQueue = launchEventQueue;
	}

	public void revealApp() {
		AppLaunchEvent launchEvent = new RevealRunningEvent();
		if (!launchEventQueue.offer(launchEvent)) {
			LOG.warn("Could not enqueue application launch event {}.", launchEvent);
		}
	}
}

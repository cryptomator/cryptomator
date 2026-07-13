package org.cryptomator.launcher;

import org.cryptomator.ipc.IpcMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Singleton
class IpcMessageHandler implements IpcMessageListener {

	private static final Logger LOG = LoggerFactory.getLogger(IpcMessageHandler.class);

	private final LaunchArgsParser launchArgsParser;
	private final BlockingQueue<AppLaunchEvent> launchEventQueue;

	@Inject
	public IpcMessageHandler(LaunchArgsParser launchArgsParser, @Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue) {
		this.launchArgsParser = launchArgsParser;
		this.launchEventQueue = launchEventQueue;
	}

	@Override
	public void revealRunningApp() {
		launchEventQueue.add(new RevealRunningEvent());
	}

	@Override
	public void handleLaunchArgs(List<String> args) {
		LOG.debug("Received launch args: {}", args);
		try {
			launchArgsParser.process(args);
		} catch (IllegalArgumentException e) {
			LOG.warn("Ignoring malformed launch args: {}", e.getMessage());
		}
	}

}

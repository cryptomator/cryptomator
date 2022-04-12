package org.cryptomator.launcher;

import org.cryptomator.ipc.IpcMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Singleton
class IpcMessageHandler implements IpcMessageListener {

	private static final Logger LOG = LoggerFactory.getLogger(IpcMessageHandler.class);

	private final FileOpenRequestHandler fileOpenRequestHandler;
	private final BlockingQueue<AppLaunchEvent> launchEventQueue;

	@Inject
	public IpcMessageHandler(FileOpenRequestHandler fileOpenRequestHandler, @Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue) {
		this.fileOpenRequestHandler = fileOpenRequestHandler;
		this.launchEventQueue = launchEventQueue;
	}

	@Override
	public void revealRunningApp() {
		launchEventQueue.add(new AppLaunchEvent(AppLaunchEvent.EventType.REVEAL_APP, Collections.emptyList()));
	}

	@Override
	public void handleLaunchArgs(List<String> args) {
		LOG.debug("Received launch args: {}", args.stream().reduce((a, b) -> a + ", " + b).orElse(""));
		fileOpenRequestHandler.handleLaunchArgs(args);
	}

}

package org.cryptomator.launcher;

import org.cryptomator.ui.launcher.AppLaunchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;

@Singleton
class IpcProtocolImpl implements IpcProtocol {

	private static final Logger LOG = LoggerFactory.getLogger(IpcProtocolImpl.class);

	private final FileOpenRequestHandler fileOpenRequestHandler;
	private final BlockingQueue<AppLaunchEvent> launchEventQueue;

	@Inject
	public IpcProtocolImpl(FileOpenRequestHandler fileOpenRequestHandler, @Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue) {
		this.fileOpenRequestHandler = fileOpenRequestHandler;
		this.launchEventQueue = launchEventQueue;
	}

	@Override
	public void revealRunningApp() {
		launchEventQueue.add(new AppLaunchEvent(AppLaunchEvent.EventType.REVEAL_APP, Collections.emptyList()));
	}

	@Override
	public void handleLaunchArgs(String... args) {
		LOG.debug("Received launch args: {}", Arrays.stream(args).reduce((a, b) -> a + ", " + b).orElse(""));
		fileOpenRequestHandler.handleLaunchArgs(args);
	}

}

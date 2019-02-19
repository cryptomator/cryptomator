package org.cryptomator.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

@Singleton
class IpcProtocolImpl implements IpcProtocol {

	private static final Logger LOG = LoggerFactory.getLogger(IpcProtocolImpl.class);

	private final FileOpenRequestHandler fileOpenRequestHandler;

	@Inject
	public IpcProtocolImpl(FileOpenRequestHandler fileOpenRequestHandler) {
		this.fileOpenRequestHandler = fileOpenRequestHandler;
	}

	@Override
	public void handleLaunchArgs(String[] args) {
		LOG.info("Received launch args: {}", Arrays.stream(args).reduce((a, b) -> a + ", " + b).orElse(""));
		fileOpenRequestHandler.handleLaunchArgs(args);
	}

}

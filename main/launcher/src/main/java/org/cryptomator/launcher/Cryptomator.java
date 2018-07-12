/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;

public class Cryptomator {

	private static final Logger LOG = LoggerFactory.getLogger(Cryptomator.class);
	static final BlockingQueue<Path> FILE_OPEN_REQUESTS = new ArrayBlockingQueue<>(10);

	public static void main(String[] args) {
		LOG.info("Starting Cryptomator {} on {} {} ({})", ApplicationVersion.orElse("SNAPSHOT"), SystemUtils.OS_NAME, SystemUtils.OS_VERSION, SystemUtils.OS_ARCH);

		FileOpenRequestHandler fileOpenRequestHandler = new FileOpenRequestHandler(FILE_OPEN_REQUESTS);
		try (InterProcessCommunicator communicator = InterProcessCommunicator.start(new IpcProtocolImpl(fileOpenRequestHandler))) {
			if (communicator.isServer()) {
				fileOpenRequestHandler.handleLaunchArgs(args);
				CleanShutdownPerformer.registerShutdownHook();
				Application.launch(MainApplication.class, args);
			} else {
				communicator.handleLaunchArgs(args);
				LOG.info("Found running application instance. Shutting down.");
			}
		} catch (IOException e) {
			LOG.error("Failed to initiate inter-process communication.", e);
		} catch (Throwable e) {
			LOG.error("Error during startup", e);
		}
		System.exit(0); // end remaining non-daemon threads.
	}

	private static class IpcProtocolImpl implements InterProcessCommunicationProtocol {

		private final FileOpenRequestHandler fileOpenRequestHandler;

		// TODO: inject?
		public IpcProtocolImpl(FileOpenRequestHandler fileOpenRequestHandler) {
			this.fileOpenRequestHandler = fileOpenRequestHandler;
		}

		@Override
		public void handleLaunchArgs(String[] args) {
			LOG.info("Received launch args: {}", Arrays.stream(args).reduce((a, b) -> a + ", " + b).orElse(""));
			fileOpenRequestHandler.handleLaunchArgs(args);
		}

	}

}

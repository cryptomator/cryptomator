/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved.
 * 
 * This class is licensed under the LGPL 3.0 (https://www.gnu.org/licenses/lgpl-3.0.de.html).
 *******************************************************************************/
package org.cryptomator.launcher;

import org.cryptomator.ui.model.AppLaunchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.awt.Desktop;
import java.awt.desktop.OpenFilesEvent;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

@Singleton
class FileOpenRequestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(FileOpenRequestHandler.class);
	private final BlockingQueue<AppLaunchEvent> launchEventQueue;

	@Inject
	public FileOpenRequestHandler(@Named("launchEventQueue") BlockingQueue<AppLaunchEvent> launchEventQueue) {
		this.launchEventQueue = launchEventQueue;
		try {
			Desktop.getDesktop().setOpenFileHandler(this::openFiles);
		} catch (UnsupportedOperationException e) {
			LOG.info("Unable to setOpenFileHandler, probably not supported on this OS.");
		}
	}

	private void openFiles(final OpenFilesEvent evt) {
		Stream<Path> pathsToOpen = evt.getFiles().stream().map(File::toPath);
		AppLaunchEvent launchEvent = new AppLaunchEvent(pathsToOpen);
		tryToEnqueueFileOpenRequest(launchEvent);
	}

	public void handleLaunchArgs(String[] args) {
		handleLaunchArgs(FileSystems.getDefault(), args);
	}

	// visible for testing
	void handleLaunchArgs(FileSystem fs, String[] args) {
		Stream<Path> pathsToOpen = Arrays.stream(args).map(str -> {
			try {
				return fs.getPath(str);
			} catch (InvalidPathException e) {
				LOG.trace("Argument not a valid path: {}", str);
				return null;
			}
		}).filter(Objects::nonNull);
		AppLaunchEvent launchEvent = new AppLaunchEvent(pathsToOpen);
		tryToEnqueueFileOpenRequest(launchEvent);
	}


	private void tryToEnqueueFileOpenRequest(AppLaunchEvent launchEvent) {
		if (!launchEventQueue.offer(launchEvent)) {
			LOG.warn("Could not enqueue application launch event.", launchEvent);
		}
	}

}

/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved.
 * 
 * This class is licensed under the LGPL 3.0 (https://www.gnu.org/licenses/lgpl-3.0.de.html).
 *******************************************************************************/
package org.cryptomator.launcher;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileOpenRequestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(FileOpenRequestHandler.class);
	private final BlockingQueue<Path> fileOpenRequests;

	public FileOpenRequestHandler(BlockingQueue<Path> fileOpenRequests) {
		this.fileOpenRequests = fileOpenRequests;
		Desktop.getDesktop().setOpenFileHandler(e -> {
			e.getFiles().stream().map(File::toPath).forEach(fileOpenRequests::add);
		});
	}

	public void handleLaunchArgs(String[] args) {
		handleLaunchArgs(FileSystems.getDefault(), args);
	}

	// visible for testing
	void handleLaunchArgs(FileSystem fs, String[] args) {
		for (String arg : args) {
			try {
				Path path = fs.getPath(arg);
				tryToEnqueueFileOpenRequest(path);
			} catch (InvalidPathException e) {
				LOG.trace("{} not a valid path", arg);
			}
		}
	}

	private void tryToEnqueueFileOpenRequest(Path path) {
		if (!fileOpenRequests.offer(path)) {
			LOG.warn("{} could not be enqueued for opening.", path);
		}
	}

}

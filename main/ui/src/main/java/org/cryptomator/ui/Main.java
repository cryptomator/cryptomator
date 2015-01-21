/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javafx.application.Application;

import org.controlsfx.tools.Platform;
import org.cryptomator.ui.util.SingleInstanceManager;
import org.cryptomator.ui.util.SingleInstanceManager.RemoteInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.axet.desktop.os.mac.AppleHandlers;

public class Main {
	public static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);

	public static final CompletableFuture<Consumer<File>> openFileHandler = new CompletableFuture<>();

	public static void main(String[] args) {
		if (Platform.getCurrent().equals(org.controlsfx.tools.Platform.OSX)) {
			/*
			 * On OSX we're in an awkward position. We need to register a
			 * handler in the main thread of this application. However, we can't
			 * even pass objects to the application, so we're forced to use a
			 * static CompletableFuture for the handler, which actually opens
			 * the file in the application.
			 */
			try {
				AppleHandlers.getAppleHandlers().addOpenFileListener(list -> {
					try {
						openFileHandler.get().accept(list);
					} catch (Exception e) {
						LOG.error("exception handling file open event", e);
						throw new RuntimeException(e);
					}
				});
			} catch (Throwable e) {
				// Since we're trying to call OS-specific code, we'll just have
				// to hope for the best.
				LOG.error("exception adding OSX file open handler", e);
			}
		}

		/*
		 * Before starting the application, we check if there is already an
		 * instance running on this computer. If so, we send our command line
		 * arguments to that instance and quit.
		 */
		final Optional<RemoteInstance> remoteInstance = SingleInstanceManager.getRemoteInstance(MainApplication.APPLICATION_KEY);

		if (remoteInstance.isPresent()) {
			try (RemoteInstance instance = remoteInstance.get()) {
				LOG.info("An instance of Cryptomator is already running at {}.", instance.getRemotePort());
				for (int i = 0; i < args.length; i++) {
					remoteInstance.get().sendMessage(args[i], 1000);
				}
			} catch (Exception e) {
				LOG.error("Error forwarding arguments to remote instance", e);
			}
		} else {
			Application.launch(MainApplication.class, args);
		}
	}
}

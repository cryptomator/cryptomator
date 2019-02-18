/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.controllers.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Cryptomator {

	private static final Logger LOG = LoggerFactory.getLogger(Cryptomator.class);
	private static final CryptomatorComponent CRYPTOMATOR_COMPONENT = DaggerCryptomatorComponent.create();
	private static final Path DEFAULT_IPC_PATH = Paths.get(".ipcPort.tmp");

	// We need a separate FX Application class.
	// If org.cryptomator.launcher.Cryptomator simply extended Application, the module system magically kicks in and throws exceptions
	public static class MainApp extends Application {

		private Stage primaryStage;

		@Override
		public void start(Stage primaryStage) {
			LOG.info("JavaFX application started.");
			this.primaryStage = primaryStage;
			primaryStage.setMinWidth(652.0);
			primaryStage.setMinHeight(440.0);

			FxApplicationComponent fxApplicationComponent = CRYPTOMATOR_COMPONENT.fxApplicationComponent() //
					.fxApplication(this) //
					.mainWindow(primaryStage) //
					.build();

			fxApplicationComponent.debugMode().initialize();

			MainController mainCtrl = fxApplicationComponent.fxmlLoader().load("/fxml/main.fxml");
			mainCtrl.initStage(primaryStage);
			primaryStage.show();
		}

		@Override
		public void stop() {
			assert primaryStage != null;
			primaryStage.hide();
			LOG.info("JavaFX application stopped.");
		}

	}

	public static void main(String[] args) {
		LOG.info("Starting Cryptomator {} on {} {} ({})", CRYPTOMATOR_COMPONENT.applicationVersion().orElse("SNAPSHOT"), SystemUtils.OS_NAME, SystemUtils.OS_VERSION, SystemUtils.OS_ARCH);

		FileOpenRequestHandler fileOpenRequestHandler = CRYPTOMATOR_COMPONENT.fileOpenRequestHanlder();
		Path ipcPortPath = CRYPTOMATOR_COMPONENT.environment().getIpcPortPath().findFirst().orElse(DEFAULT_IPC_PATH);
		try (InterProcessCommunicator communicator = InterProcessCommunicator.start(ipcPortPath, new IpcProtocolImpl(fileOpenRequestHandler))) {
			if (communicator.isServer()) {
				fileOpenRequestHandler.handleLaunchArgs(args);
				CleanShutdownPerformer.registerShutdownHook();
				Application.launch(MainApp.class, args);
			} else {
				communicator.handleLaunchArgs(args);
				LOG.info("Found running application instance. Shutting down.");
			}
			System.exit(0); // end remaining non-daemon threads.
		} catch (IOException e) {
			LOG.error("Failed to initiate inter-process communication.", e);
			System.exit(2);
		} catch (Throwable e) {
			LOG.error("Error during startup", e);
			System.exit(1);
		}
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

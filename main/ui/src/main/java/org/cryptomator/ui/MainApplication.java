/*******************************************************************************
 * Copyright (c) 2014, 2016 Sebastian Stenzel
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 * 
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 ******************************************************************************/
package org.cryptomator.ui;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.cryptolib.common.SecureRandomModule;
import org.cryptomator.ui.controllers.MainController;
import org.cryptomator.ui.util.ActiveWindowStyleSupport;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.SingleInstanceManager;
import org.cryptomator.ui.util.SingleInstanceManager.LocalInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainApplication extends Application {

	public static final String APPLICATION_KEY = "CryptomatorGUI";
	private static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);

	private DeferredCloser closer;

	@Override
	public void start(Stage primaryStage) throws IOException {
		LOG.info("JavaFX application started");

		CryptomatorComponent comp = createCryptomatorComponent(primaryStage);
		MainController mainCtrl = comp.mainController();
		closer = comp.deferredCloser();

		comp.debugMode().initialize();

		setupFXMLClassLoader();
		setupStylesheets();

		initializeStage(primaryStage, mainCtrl);
		showWindow(primaryStage);

		registerExitHandler(comp);

		openFilesRequestedDuringStartup(primaryStage, mainCtrl);
		registerApplicationToProcessOpenFileRequests(primaryStage, comp, mainCtrl);
	}

	@Override
	public void stop() {
		try {
			closer.close();
		} catch (ExecutionException e) {
			LOG.error("Error closing ressources", e);
		}
	}

	private CryptomatorComponent createCryptomatorComponent(Stage primaryStage) {
		try {
			return DaggerCryptomatorComponent.builder() //
					.cryptomatorModule(new CryptomatorModule(this, primaryStage)) //
					.secureRandomModule(new SecureRandomModule(SecureRandom.getInstanceStrong())) //
					.build();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every implementation of the Java platform is required to support at least one strong SecureRandom implementation.", e);
		}
	}

	private void setupFXMLClassLoader() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		FXMLLoader.setDefaultClassLoader(contextClassLoader);
		Platform.runLater(() -> {
			/*
			 * This fixes a bug on OSX where the magic file open handler leads to no context class loader being set in the AppKit (event)
			 * thread if the application is not started opening a file.
			 */
			if (Thread.currentThread().getContextClassLoader() == null) {
				Thread.currentThread().setContextClassLoader(contextClassLoader);
			}
		});
	}

	private void setupStylesheets() {
		Font.loadFont(getClass().getResourceAsStream("/css/ionicons.ttf"), 12.0);
		chooseNativeStylesheet();
	}

	private void initializeStage(Stage primaryStage, MainController mainCtrl) {
		mainCtrl.initStage(primaryStage);
		primaryStage.titleProperty().bind(mainCtrl.windowTitle());
		primaryStage.setResizable(false);
		if (SystemUtils.IS_OS_WINDOWS) {
			primaryStage.getIcons().add(new Image(MainApplication.class.getResourceAsStream("/window_icon.png")));
		}
	}

	private void showWindow(Stage primaryStage) {
		primaryStage.show();
		ActiveWindowStyleSupport.startObservingFocus(primaryStage);
	}

	private void registerExitHandler(CryptomatorComponent comp) {
		comp.exitUtil().initExitHandler(this::quit);
	}

	private void openFilesRequestedDuringStartup(Stage primaryStage, final MainController mainCtrl) {
		for (String arg : getParameters().getUnnamed()) {
			handleCommandLineArg(arg, primaryStage, mainCtrl);
		}
		if (SystemUtils.IS_OS_MAC_OSX) {
			Cryptomator.OPEN_FILE_HANDLER.complete(file -> handleCommandLineArg(file.getAbsolutePath(), primaryStage, mainCtrl));
		}
	}

	private void registerApplicationToProcessOpenFileRequests(Stage primaryStage, final CryptomatorComponent comp, final MainController mainCtrl) throws IOException {
		LocalInstance cryptomatorGuiInstance = closer.closeLater(SingleInstanceManager.startLocalInstance(APPLICATION_KEY, comp.executorService()), LocalInstance::close).get().get();
		cryptomatorGuiInstance.registerListener(arg -> handleCommandLineArg(arg, primaryStage, mainCtrl));
	}

	private void chooseNativeStylesheet() {
		if (SystemUtils.IS_OS_MAC_OSX) {
			setUserAgentStylesheet(getClass().getResource("/css/mac_theme.css").toString());
		} else if (SystemUtils.IS_OS_LINUX) {
			setUserAgentStylesheet(getClass().getResource("/css/linux_theme.css").toString());
		} else if (SystemUtils.IS_OS_WINDOWS) {
			setUserAgentStylesheet(getClass().getResource("/css/win_theme.css").toString());
		}
	}

	private void handleCommandLineArg(String arg, Stage primaryStage, MainController mainCtrl) {
		// find correct location:
		final Path path = FileSystems.getDefault().getPath(arg);
		final Path vaultPath;
		if (Files.isDirectory(path)) {
			vaultPath = path;
		} else if (Files.isRegularFile(path)) {
			vaultPath = path.getParent();
		} else {
			LOG.warn("Invalid vault path %s", arg);
			return;
		}

		// add vault to ctrl:
		Platform.runLater(() -> {
			mainCtrl.addVault(vaultPath, true);
			primaryStage.setIconified(false);
			primaryStage.show();
			primaryStage.toFront();
			primaryStage.requestFocus();
		});
	}

	private void quit() {
		Platform.runLater(() -> {
			stop();
			Platform.exit();
			System.exit(0);
		});
	}

}

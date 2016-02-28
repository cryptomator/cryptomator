/*******************************************************************************
 * Copyright (c) 2014, 2015 Sebastian Stenzel
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
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.controllers.MainController;
import org.cryptomator.ui.util.ActiveWindowStyleSupport;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.SingleInstanceManager;
import org.cryptomator.ui.util.SingleInstanceManager.LocalInstance;
import org.cryptomator.ui.util.TrayIconUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApplication extends Application {

	public static final String APPLICATION_KEY = "CryptomatorGUI";

	private static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);

	private final ExecutorService executorService;
	private final DeferredCloser closer;
	private final MainController mainCtrl;

	public MainApplication() {
		final CryptomatorComponent comp = DaggerCryptomatorComponent.builder().cryptomatorModule(new CryptomatorModule(this)).build();
		this.executorService = comp.executorService();
		this.closer = comp.deferredCloser();
		this.mainCtrl = comp.mainController();
		Cryptomator.addShutdownTask(closer::close);
	}

	@Override
	public void start(final Stage primaryStage) throws IOException {
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

		chooseNativeStylesheet();

		mainCtrl.initStage(primaryStage);

		final ResourceBundle rb = ResourceBundle.getBundle("localization");
		primaryStage.setTitle(rb.getString("app.name"));
		primaryStage.setResizable(false);
		primaryStage.getIcons().add(new Image(MainApplication.class.getResourceAsStream("/window_icon.png"))); 
		primaryStage.show();

		ActiveWindowStyleSupport.startObservingFocus(primaryStage);
		TrayIconUtil.init(primaryStage, rb, () -> {
			quit();
		});

		for (String arg : getParameters().getUnnamed()) {
			handleCommandLineArg(arg);
		}

		if (SystemUtils.IS_OS_MAC_OSX) {
			Cryptomator.OPEN_FILE_HANDLER.complete(file -> handleCommandLineArg(file.getAbsolutePath()));
		}

		LocalInstance cryptomatorGuiInstance = closer.closeLater(SingleInstanceManager.startLocalInstance(APPLICATION_KEY, executorService), LocalInstance::close).get().get();
		cryptomatorGuiInstance.registerListener(arg -> handleCommandLineArg(arg));
	}

	private void handleCommandLineArg(String arg) {
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
			mainCtrl.toFront();
		});
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

	private void quit() {
		Platform.runLater(() -> {
			stop();
			Platform.exit();
			System.exit(0);
		});
	}

	@Override
	public void stop() {
		closer.close();
	}

}

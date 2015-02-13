/*******************************************************************************
 * Copyright (c) 2014 Sebastian Stenzel
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
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.ActiveWindowStyleSupport;
import org.cryptomator.ui.util.SingleInstanceManager;
import org.cryptomator.ui.util.SingleInstanceManager.LocalInstance;
import org.cryptomator.ui.util.TrayIconUtil;
import org.cryptomator.webdav.WebDavServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApplication extends Application {

	public static final String APPLICATION_KEY = "CryptomatorGUI";

	private static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);

	private ExecutorService executorService;

	@Override
	public void start(final Stage primaryStage) throws IOException {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Platform.runLater(() -> {
			/*
			 * This fixes a bug on OSX where the magic file open handler leads to no context class loader being set in the AppKit (event)
			 * thread if the application is not started opening a file.
			 */
			if (Thread.currentThread().getContextClassLoader() == null) {
				Thread.currentThread().setContextClassLoader(contextClassLoader);
			}
		});

		executorService = Executors.newCachedThreadPool();

		WebDavServer.getInstance().start();
		chooseNativeStylesheet();
		final ResourceBundle rb = ResourceBundle.getBundle("localization");
		final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"), rb);
		final Parent root = loader.load();
		final MainController ctrl = loader.getController();
		ctrl.setStage(primaryStage);
		final Scene scene = new Scene(root);
		primaryStage.setTitle(rb.getString("app.name"));
		primaryStage.setScene(scene);
		primaryStage.sizeToScene();
		primaryStage.setResizable(false);
		primaryStage.show();
		ActiveWindowStyleSupport.startObservingFocus(primaryStage);
		TrayIconUtil.init(primaryStage, rb, () -> {
			quit();
		});

		for (String arg : getParameters().getUnnamed()) {
			handleCommandLineArg(ctrl, arg);
		}

		if (SystemUtils.IS_OS_MAC_OSX) {
			Main.OPEN_FILE_HANDLER.complete(file -> handleCommandLineArg(ctrl, file.getAbsolutePath()));
		}

		final LocalInstance cryptomatorGuiInstance = SingleInstanceManager.startLocalInstance(APPLICATION_KEY, executorService);
		cryptomatorGuiInstance.registerListener(arg -> handleCommandLineArg(ctrl, arg));

		Main.addShutdownTask(() -> {
			cryptomatorGuiInstance.close();
			Settings.save();
			executorService.shutdown();
		});
	}

	void handleCommandLineArg(final MainController ctrl, String arg) {
		// only open files with our file extension:
		if (!arg.endsWith(Vault.VAULT_FILE_EXTENSION)) {
			LOG.warn("Invalid vault path %s", arg);
			return;
		}

		// find correct location:
		final Path path = FileSystems.getDefault().getPath(arg);
		final Path vaultPath;
		if (Files.isDirectory(path)) {
			vaultPath = path;
		} else if (Files.isRegularFile(path) && path.getParent().getFileName().toString().endsWith(Vault.VAULT_FILE_EXTENSION)) {
			vaultPath = path.getParent();
		} else {
			LOG.warn("Invalid vault path %s", arg);
			return;
		}

		// add vault to ctrl:
		Platform.runLater(() -> {
			ctrl.addVault(vaultPath, true);
			ctrl.toFront();
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
			WebDavServer.getInstance().stop();
			Settings.save();
			Platform.exit();
			System.exit(0);
		});
	}

}

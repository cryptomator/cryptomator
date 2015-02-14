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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.MainModule.ControllerFactory;
import org.cryptomator.ui.util.ActiveWindowStyleSupport;
import org.cryptomator.ui.util.DeferredCloser;
import org.cryptomator.ui.util.SingleInstanceManager;
import org.cryptomator.ui.util.SingleInstanceManager.LocalInstance;
import org.cryptomator.ui.util.TrayIconUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class MainApplication extends Application {

	public static final String APPLICATION_KEY = "CryptomatorGUI";

	private static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);

	private final CleanShutdownPerformer cleanShutdownPerformer = new CleanShutdownPerformer();

	private final ExecutorService executorService;

	private final ControllerFactory controllerFactory;

	private final DeferredCloser closer;

	public MainApplication() {
		this(getInjector());
	}

	private static Injector getInjector() {
		try {
			return Guice.createInjector(new MainModule());
		} catch (Exception e) {
			throw e;
		}
	}

	public MainApplication(Injector injector) {
		this(injector.getInstance(ExecutorService.class),
				injector.getInstance(ControllerFactory.class),
				injector.getInstance(DeferredCloser.class));
	}

	public MainApplication(ExecutorService executorService, ControllerFactory controllerFactory, DeferredCloser closer) {
		super();
		this.executorService = executorService;
		this.controllerFactory = controllerFactory;
		this.closer = closer;
	}

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

		Runtime.getRuntime().addShutdownHook(cleanShutdownPerformer);

		chooseNativeStylesheet();
		final ResourceBundle rb = ResourceBundle.getBundle("localization");
		final FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"), rb);
		loader.setControllerFactory(controllerFactory);
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

		LocalInstance cryptomatorGuiInstance = closer.closeLater(
				SingleInstanceManager.startLocalInstance(APPLICATION_KEY, executorService), LocalInstance::close).get().get();

		cryptomatorGuiInstance.registerListener(arg -> handleCommandLineArg(ctrl, arg));
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
			stop();
			Platform.exit();
			System.exit(0);
		});
	}

	@Override
	public void stop() {
		closer.close();
		try {
			Runtime.getRuntime().removeShutdownHook(cleanShutdownPerformer);
		} catch (Exception e) {

		}
	}

	private class CleanShutdownPerformer extends Thread {
		@Override
		public void run() {
			closer.close();
		}
	}

}

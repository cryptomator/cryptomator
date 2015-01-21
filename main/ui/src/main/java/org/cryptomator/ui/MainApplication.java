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
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.crypto.aes256.Aes256Cryptor;
import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.ActiveWindowStyleSupport;
import org.cryptomator.ui.util.SingleInstanceManager;
import org.cryptomator.ui.util.SingleInstanceManager.LocalInstance;
import org.cryptomator.ui.util.TrayIconUtil;
import org.cryptomator.webdav.WebDavServer;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApplication extends Application {

	private static final Set<Runnable> SHUTDOWN_TASKS = new ConcurrentHashSet<>();
	private static final CleanShutdownPerformer CLEAN_SHUTDOWN_PERFORMER = new CleanShutdownPerformer();

	public static final String APPLICATION_KEY = "CryptomatorGUI";

	private static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);

	ExecutorService executorService;

	@Override
	public void start(final Stage primaryStage) throws IOException {
		Runtime.getRuntime().addShutdownHook(MainApplication.CLEAN_SHUTDOWN_PERFORMER);

		executorService = Executors.newCachedThreadPool();
		addShutdownTask(() -> {
			executorService.shutdown();
		});

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

		if (org.controlsfx.tools.Platform.getCurrent().equals(org.controlsfx.tools.Platform.OSX)) {
			Main.openFileHandler.complete(file -> handleCommandLineArg(ctrl, file.getAbsolutePath()));
		}

		LocalInstance cryptomatorGuiInstance = SingleInstanceManager.startLocalInstance(APPLICATION_KEY, executorService);
		addShutdownTask(() -> {
			cryptomatorGuiInstance.close();
		});

		cryptomatorGuiInstance.registerListener(arg -> handleCommandLineArg(ctrl, arg));
	}

	void handleCommandLineArg(final MainController ctrl, String arg) {
		File file = new File(arg);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				return;
			}
			// directory created.
		} else if (file.isFile()) {
			if (file.getName().toLowerCase().endsWith(Aes256Cryptor.MASTERKEY_FILE_EXT.toLowerCase())) {
				file = file.getParentFile();
			} else {
				// is a file, but not a masterkey file
				return;
			}
		}
		File f = file;
		Platform.runLater(() -> {
			ctrl.addDirectory(f);
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
			CLEAN_SHUTDOWN_PERFORMER.run();
			Settings.save();
			Platform.exit();
			System.exit(0);
		});
	}

	@Override
	public void stop() {
		CLEAN_SHUTDOWN_PERFORMER.run();
		Settings.save();
	}

	public static void addShutdownTask(Runnable r) {
		SHUTDOWN_TASKS.add(r);
	}

	public static void removeShutdownTask(Runnable r) {
		SHUTDOWN_TASKS.remove(r);
	}

	private static class CleanShutdownPerformer extends Thread {
		@Override
		public void run() {
			SHUTDOWN_TASKS.forEach(r -> {
				try {
					r.run();
				} catch (Throwable e) {
					LOG.error("exception while shutting down", e);
				}
			});
			SHUTDOWN_TASKS.clear();
		}
	}

}

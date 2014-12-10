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
import java.util.ResourceBundle;
import java.util.Set;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.TrayIconUtil;
import org.eclipse.jetty.util.ConcurrentHashSet;

public class MainApplication extends Application {

	private static final Set<Runnable> SHUTDOWN_TASKS = new ConcurrentHashSet<>();
	private static final CleanShutdownPerformer CLEAN_SHUTDOWN_PERFORMER = new CleanShutdownPerformer();

	public static void main(String[] args) {
		Application.launch(args);
		Runtime.getRuntime().addShutdownHook(CLEAN_SHUTDOWN_PERFORMER);
	}

	@Override
	public void start(final Stage primaryStage) throws IOException {
		final ResourceBundle rb = ResourceBundle.getBundle("localization");
		final FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"), rb);
		final Parent root = loader.load();
		final MainController ctrl = loader.getController();
		ctrl.setStage(primaryStage);
		final Scene scene = new Scene(root);
		primaryStage.setTitle(rb.getString("app.name"));
		primaryStage.setScene(scene);
		primaryStage.sizeToScene();
		primaryStage.setResizable(false);
		primaryStage.show();
		TrayIconUtil.init(primaryStage, rb, () -> {
			quit();
		});
	}

	private void quit() {
		Platform.runLater(() -> {
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
				r.run();
			});
			SHUTDOWN_TASKS.clear();
		}
	}

}

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
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.cryptomator.ui.settings.Settings;
import org.eclipse.jetty.util.ConcurrentHashSet;

public class MainApplication extends Application {

	private static final Set<Runnable> SHUTDOWN_TASKS = new ConcurrentHashSet<>();
	private static final CleanShutdownPerformer CLEAN_SHUTDOWN_PERFORMER = new CleanShutdownPerformer();

	public static void main(String[] args) {
		launch(args);
		Runtime.getRuntime().addShutdownHook(CLEAN_SHUTDOWN_PERFORMER);
	}

	@Override
	public void start(final Stage primaryStage) throws IOException {
		final ResourceBundle localizations = ResourceBundle.getBundle("localization");
		final Parent root = FXMLLoader.load(getClass().getResource("/main.fxml"), localizations);
		final Scene scene = new Scene(root);
		primaryStage.setTitle("Cryptomator");
		primaryStage.setScene(scene);
		primaryStage.sizeToScene();
		primaryStage.setResizable(false);
		primaryStage.show();
	}

	@Override
	public void stop() throws Exception {
		CLEAN_SHUTDOWN_PERFORMER.run();
		Settings.save();
		super.stop();
	}

	static void addShutdownTask(Runnable r) {
		SHUTDOWN_TASKS.add(r);
	}

	static void removeShutdownTask(Runnable r) {
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

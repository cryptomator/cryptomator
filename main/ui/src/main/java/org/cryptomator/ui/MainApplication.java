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

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.cryptomator.ui.settings.Settings;
import org.cryptomator.ui.util.WebDavMounter;
import org.cryptomator.ui.util.WebDavMounter.CommandFailedException;
import org.cryptomator.webdav.WebDAVServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);

	public static void main(String[] args) {
		launch(args);
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
		try {
			WebDavMounter.unmount(5);
		} catch (CommandFailedException e) {
			LOG.warn("Unmounting WebDAV share failed.", e);
		}
		WebDAVServer.getInstance().stop();
		Settings.save();
		super.stop();
	}

}

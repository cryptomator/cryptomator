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
import org.cryptomator.webdav.WebDAVServer;

public class MainApplication extends Application {

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
		WebDAVServer.getInstance().stop();
		Settings.save();
		super.stop();
	}

}

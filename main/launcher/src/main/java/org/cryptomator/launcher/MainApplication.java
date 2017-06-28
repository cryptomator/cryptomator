/*******************************************************************************
 * Copyright (c) 2017 Skymatic UG (haftungsbeschränkt).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the accompanying LICENSE file.
 *******************************************************************************/
package org.cryptomator.launcher;

import org.cryptomator.ui.controllers.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class MainApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);
	private Stage primaryStage;

	@Override
	public void start(Stage primaryStage) throws Exception {
		LOG.info("JavaFX application started.");
		this.primaryStage = primaryStage;
		setupFXMLClassLoader();

		LauncherModule launcherModule = new LauncherModule(this, primaryStage);
		LauncherComponent launcherComponent = DaggerLauncherComponent.builder() //
				.launcherModule(launcherModule) //
				.build();

		launcherComponent.debugMode().initialize();

		MainController mainCtrl = launcherComponent.fxmlLoader().load("/fxml/main.fxml");
		mainCtrl.initStage(primaryStage);

		primaryStage.show();
	}

	@Override
	public void stop() throws Exception {
		assert primaryStage != null;
		primaryStage.hide();
		LOG.info("JavaFX application stopped.");
	}

	// fix discussed in https://github.com/cryptomator/cryptomator/pull/29
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

}

package org.cryptomator.launcher;

import javafx.application.Application;
import javafx.stage.Stage;
import org.cryptomator.common.FxApplicationScoped;
import org.cryptomator.ui.controllers.MainController;
import org.cryptomator.ui.controllers.ViewControllerLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

@FxApplicationScoped
public class FxApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);

	private final Stage primaryStage;
	private final ViewControllerLoader fxmlLoader;

	@Inject
	FxApplication(@Named("mainWindow") Stage primaryStage, ViewControllerLoader fxmlLoader) {
		this.primaryStage = primaryStage;
		this.fxmlLoader = fxmlLoader;
	}

	public void start() {
		LOG.info("Starting GUI...");
		start(primaryStage);
	}

	@Override
	public void start(Stage primaryStage) {
		MainController mainCtrl = fxmlLoader.load("/fxml/main.fxml");
		mainCtrl.initStage(primaryStage);
		primaryStage.show();
	}

}

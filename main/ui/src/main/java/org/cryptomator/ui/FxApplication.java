package org.cryptomator.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.mainwindow.MainWindow;
import org.cryptomator.ui.preferences.PreferencesWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.Desktop;
import java.awt.desktop.PreferencesEvent;
import java.io.IOException;
import java.io.UncheckedIOException;

@FxApplicationScoped
public class FxApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);

	private final Stage mainWindow;
	private final Stage preferencesWindow;
	private final FXMLLoaderFactory fxmlLoaders;

	@Inject
	FxApplication(@MainWindow Stage mainWindow, @PreferencesWindow Stage preferencesWindow, FXMLLoaderFactory fxmlLoaders) {
		this.mainWindow = mainWindow;
		this.preferencesWindow = preferencesWindow;
		this.fxmlLoaders = fxmlLoaders;
	}

	public void start() {
		LOG.trace("FxApplication.start()");
		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
			Desktop.getDesktop().setPreferencesHandler(this::handlePreferences);
		}
		
		start(mainWindow);
	}

	@Override
	public void start(Stage stage) {
		assert stage == mainWindow;
		showMainWindow();
	}

	private void handlePreferences(PreferencesEvent preferencesEvent) {
		Platform.runLater(this::showPreferencesWindow);
	}

	public void showMainWindow() {
		showViewInWindow("/fxml/main_window.fxml", mainWindow);
	}

	public void showPreferencesWindow() {
		showViewInWindow("/fxml/preferences.fxml", preferencesWindow);
	}
	
	private void showViewInWindow(String fxmlResourceName, Stage window) {
		try {
			Parent root = fxmlLoaders.load(fxmlResourceName).getRoot();
			window.setScene(new Scene(root));
			window.show();
		} catch (IOException e) {
			LOG.error("Failed to load " + fxmlResourceName, e);
		}
	}

}

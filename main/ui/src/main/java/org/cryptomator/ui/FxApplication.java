package org.cryptomator.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.ui.preferences.PreferencesComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.Desktop;
import java.awt.desktop.PreferencesEvent;

@FxApplicationScoped
public class FxApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);

	private final MainWindowComponent.Builder mainWindow;
	private final PreferencesComponent.Builder preferencesWindow;

	@Inject
	FxApplication(MainWindowComponent.Builder mainWindow, PreferencesComponent.Builder preferencesWindow) {
		this.mainWindow = mainWindow;
		this.preferencesWindow = preferencesWindow;
	}

	public void start() {
		LOG.trace("FxApplication.start()");

		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
			Desktop.getDesktop().setPreferencesHandler(this::handlePreferences);
		}

		mainWindow.build().showMainWindow();
	}

	@Override
	public void start(Stage stage) {
		throw new UnsupportedOperationException("Use start() instead.");
	}

	private void handlePreferences(PreferencesEvent preferencesEvent) {
		Platform.runLater(this::showPreferencesWindow);
	}

	public void showPreferencesWindow() {
		preferencesWindow.build().showPreferencesWindow();
	}


}

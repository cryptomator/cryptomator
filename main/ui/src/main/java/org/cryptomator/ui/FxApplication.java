package org.cryptomator.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
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

	private final Settings settings;
	private final MainWindowComponent.Builder mainWindow;
	private final PreferencesComponent.Builder preferencesWindow;

	@Inject
	FxApplication(Settings settings, MainWindowComponent.Builder mainWindow, PreferencesComponent.Builder preferencesWindow) {
		this.settings = settings;
		this.mainWindow = mainWindow;
		this.preferencesWindow = preferencesWindow;
	}

	public void start() {
		LOG.trace("FxApplication.start()");

		settings.theme().addListener(this::themeChanged);
		loadSelectedStyleSheet(settings.theme().get());

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

	private void themeChanged(@SuppressWarnings("unused") ObservableValue<? extends UiTheme> observable, @SuppressWarnings("unused") UiTheme oldValue, UiTheme newValue) {
		loadSelectedStyleSheet(newValue);
	}

	private void loadSelectedStyleSheet(UiTheme theme) {
		switch (theme) {
			case CUSTOM:
				// TODO
				Application.setUserAgentStylesheet(getClass().getResource("/css/mac_theme.css").toString());
				break;
			case DARK:
				Application.setUserAgentStylesheet(getClass().getResource("/css/dark_theme.css").toString());
				break;
			case LIGHT:
			default:
				Application.setUserAgentStylesheet(getClass().getResource("/css/light_theme.css").toString());
				break;
		}
	}

}

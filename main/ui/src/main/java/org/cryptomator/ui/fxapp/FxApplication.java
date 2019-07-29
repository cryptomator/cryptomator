package org.cryptomator.ui.fxapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.jni.MacApplicationUiAppearance;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.ui.preferences.PreferencesComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.Desktop;
import java.awt.desktop.PreferencesEvent;
import java.util.Optional;

@FxApplicationScoped
public class FxApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);

	private final Settings settings;
	private final MainWindowComponent.Builder mainWindow;
	private final PreferencesComponent.Builder preferencesWindow;
	private final Optional<MacFunctions> macFunctions;

	@Inject
	FxApplication(Settings settings, MainWindowComponent.Builder mainWindow, PreferencesComponent.Builder preferencesWindow, Optional<MacFunctions> macFunctions) {
		this.settings = settings;
		this.mainWindow = mainWindow;
		this.preferencesWindow = preferencesWindow;
		this.macFunctions = macFunctions;
	}

	public void start() {
		LOG.trace("FxApplication.start()");
		Platform.setImplicitExit(false);

		settings.theme().addListener(this::themeChanged);
		loadSelectedStyleSheet(settings.theme().get());

		if (Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
			Desktop.getDesktop().setPreferencesHandler(this::handlePreferences);
		}
	}

	@Override
	public void start(Stage stage) {
		throw new UnsupportedOperationException("Use start() instead.");
	}

	private void handlePreferences(PreferencesEvent preferencesEvent) {
		showPreferencesWindow();
	}

	public void showPreferencesWindow() {
		Platform.runLater(() -> {
			preferencesWindow.build().showPreferencesWindow();
		});
	}

	public void showMainWindow() {
		Platform.runLater(() -> {
			mainWindow.build().showMainWindow();
		});
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
				macFunctions.map(MacFunctions::uiAppearance).ifPresent(MacApplicationUiAppearance::setToDarkAqua);
				break;
			case LIGHT:
			default:
				Application.setUserAgentStylesheet(getClass().getResource("/css/light_theme.css").toString());
				macFunctions.map(MacFunctions::uiAppearance).ifPresent(MacApplicationUiAppearance::setToAqua);
				break;
		}
	}

}

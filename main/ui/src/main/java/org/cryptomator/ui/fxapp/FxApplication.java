package org.cryptomator.ui.fxapp;

import dagger.Lazy;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.jni.MacApplicationUiAppearance;
import org.cryptomator.jni.MacFunctions;
import org.cryptomator.ui.mainwindow.MainWindowComponent;
import org.cryptomator.ui.preferences.PreferencesComponent;
import org.cryptomator.ui.quit.QuitComponent;
import org.cryptomator.ui.unlock.UnlockComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.desktop.QuitResponse;
import java.util.Optional;

@FxApplicationScoped
public class FxApplication extends Application {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplication.class);

	private final Settings settings;
	private final Lazy<MainWindowComponent> mainWindow;
	private final Lazy<PreferencesComponent> preferencesWindow;
	private final UnlockComponent.Builder unlockWindowBuilder;
	private final QuitComponent.Builder quitWindowBuilder;
	private final Optional<MacFunctions> macFunctions;

	@Inject
	FxApplication(Settings settings, Lazy<MainWindowComponent> mainWindow, Lazy<PreferencesComponent> preferencesWindow, UnlockComponent.Builder unlockWindowBuilder, QuitComponent.Builder quitWindowBuilder, Optional<MacFunctions> macFunctions) {
		this.settings = settings;
		this.mainWindow = mainWindow;
		this.preferencesWindow = preferencesWindow;
		this.unlockWindowBuilder = unlockWindowBuilder;
		this.quitWindowBuilder = quitWindowBuilder;
		this.macFunctions = macFunctions;
	}

	public void start() {
		LOG.trace("FxApplication.start()");
		Platform.setImplicitExit(false);

		settings.theme().addListener(this::themeChanged);
		loadSelectedStyleSheet(settings.theme().get());
	}

	@Override
	public void start(Stage stage) {
		throw new UnsupportedOperationException("Use start() instead.");
	}

	public void showPreferencesWindow() {
		Platform.runLater(() -> {
			preferencesWindow.get().showPreferencesWindow();
			LOG.debug("Showing Preferences");
		});
	}

	public void showMainWindow() {
		Platform.runLater(() -> {
			mainWindow.get().showMainWindow();
			LOG.debug("Showing MainWindow");
		});
	}

	public void showUnlockWindow(Vault vault) {
		Platform.runLater(() -> {
			unlockWindowBuilder.vault(vault).build().showUnlockWindow();
			LOG.debug("Showing UnlockWindow for {}", vault.getDisplayableName());
		});
	}

	public void showQuitWindow(QuitResponse response) {
		Platform.runLater(() -> {
			quitWindowBuilder.quitResponse(response).build().showQuitWindow();
			LOG.debug("Showing QuitWindow");
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

package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.cryptomator.ui.launcher.AppLifecycleListener;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.cryptomator.ui.traymenu.TrayMenuComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@MainWindowScoped
public class MainWindowTitleController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowTitleController.class);

	private final AppLifecycleListener appLifecycle;
	private final Stage window;
	private final FxApplication application;
	private final boolean trayMenuInitialized;
	private final UpdateChecker updateChecker;
	private final BooleanBinding updateAvailable;
	private final LicenseHolder licenseHolder;
	private final Settings settings;
	private final BooleanBinding showMinimizeButton;

	public HBox titleBar;
	private double xOffset;
	private double yOffset;

	@Inject
	MainWindowTitleController(AppLifecycleListener appLifecycle, @MainWindow Stage window, FxApplication application, TrayMenuComponent trayMenu, UpdateChecker updateChecker, LicenseHolder licenseHolder, Settings settings) {
		this.appLifecycle = appLifecycle;
		this.window = window;
		this.application = application;
		this.trayMenuInitialized = trayMenu.isInitialized();
		this.updateChecker = updateChecker;
		this.updateAvailable = updateChecker.latestVersionProperty().isNotNull();
		this.licenseHolder = licenseHolder;
		this.settings = settings;
		this.showMinimizeButton = Bindings.createBooleanBinding(this::isShowMinimizeButton, settings.showMinimizeButton(), settings.showTrayIcon());
	}

	@FXML
	public void initialize() {
		LOG.debug("init MainWindowTitleController");
		updateChecker.automaticallyCheckForUpdatesIfEnabled();
		titleBar.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();

		});
		titleBar.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY)) {
				if (event.getClickCount() == 2) {
					if (window.isFullScreen()) {
						window.setFullScreen(false);
					} else {
						window.setFullScreen(true);
					}
				}
			}
		});
		titleBar.setOnMouseDragged(event -> {
			//TODO should it be possible to move the window, when in fullscreen?
			if (window.isFullScreen()) return;
			window.setX(event.getScreenX() - xOffset);
			window.setY(event.getScreenY() - yOffset);
		});
		titleBar.setOnMouseReleased(event -> {
			saveSettings();
		});
		window.setOnCloseRequest(event -> {
			close();
			event.consume();
		});
	}
	//TODO: almost duplicate from ResizeController.class
	private void saveSettings() {
		settings.windowYPositionProperty().setValue(window.getY());
		settings.windowXPositionProperty().setValue(window.getX());
	}

	@FXML
	public void close() {
		if (trayMenuInitialized) {
			window.close();
		} else {
			appLifecycle.quit();
		}
	}

	@FXML
	public void minimize() {
		window.setIconified(true);
	}

	@FXML
	public void showPreferences() {
		application.showPreferencesWindow(SelectedPreferencesTab.ANY);
	}

	@FXML
	public void showGeneralPreferences() {
		application.showPreferencesWindow(SelectedPreferencesTab.GENERAL);
	}

	@FXML
	public void showDonationKeyPreferences() {
		application.showPreferencesWindow(SelectedPreferencesTab.CONTRIBUTE);
	}

	/* Getter/Setter */

	public LicenseHolder getLicenseHolder() {
		return licenseHolder;
	}

	public BooleanBinding updateAvailableProperty() {
		return updateAvailable;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable.get();
	}

	public boolean isTrayIconPresent() {
		return trayMenuInitialized;
	}

	public ReadOnlyBooleanProperty debugModeEnabledProperty() {
		return settings.debugMode();
	}

	public boolean isDebugModeEnabled() {
		return debugModeEnabledProperty().get();
	}

	public BooleanBinding showMinimizeButtonProperty() {
		return showMinimizeButton;
	}

	public boolean isShowMinimizeButton() {
		// always show the minimize button if no tray icon is present OR it is explicitly enabled
		return !trayMenuInitialized || settings.showMinimizeButton().get();
	}
}

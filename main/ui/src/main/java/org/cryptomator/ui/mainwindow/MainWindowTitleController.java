package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.cryptomator.ui.launcher.AppLifecycleListener;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

@MainWindowScoped
public class MainWindowTitleController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowTitleController.class);

	public HBox titleBar;

	private final AppLifecycleListener appLifecycle;
	private final Stage window;
	private final FxApplication application;
	private final boolean minimizeToSysTray;
	private final UpdateChecker updateChecker;
	private final BooleanBinding updateAvailable;
	private final LicenseHolder licenseHolder;
	private final Settings settings;
	private final BooleanBinding debugModeEnabled;

	private double xOffset;
	private double yOffset;

	@Inject
	MainWindowTitleController(AppLifecycleListener appLifecycle, @MainWindow Stage window, FxApplication application, @Named("trayMenuSupported") boolean minimizeToSysTray, UpdateChecker updateChecker, LicenseHolder licenseHolder, Settings settings) {
		this.appLifecycle = appLifecycle;
		this.window = window;
		this.application = application;
		this.minimizeToSysTray = minimizeToSysTray;
		this.updateChecker = updateChecker;
		this.updateAvailable = updateChecker.latestVersionProperty().isNotNull();
		this.licenseHolder = licenseHolder;
		this.settings = settings;
		this.debugModeEnabled = Bindings.createBooleanBinding(this::isDebugModeEnabled, settings.debugMode());
	}

	@FXML
	public void initialize() {
		LOG.debug("init MainWindowTitleController");
		updateChecker.automaticallyCheckForUpdatesIfEnabled();
		titleBar.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
		});
		titleBar.setOnMouseDragged(event -> {
			window.setX(event.getScreenX() - xOffset);
			window.setY(event.getScreenY() - yOffset);
		});
		window.setOnCloseRequest(event -> {
			close();
			event.consume();
		});
	}

	@FXML
	public void close() {
		if (minimizeToSysTray) {
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
		application.showPreferencesWindow(SelectedPreferencesTab.DONATION_KEY);
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

	public boolean isMinimizeToSysTray() {
		return minimizeToSysTray;
	}

	public BooleanBinding debugModeEnabledProperty() {
		return debugModeEnabled;
	}

	public boolean isDebugModeEnabled() {
		return settings.debugMode().get();
	}
}

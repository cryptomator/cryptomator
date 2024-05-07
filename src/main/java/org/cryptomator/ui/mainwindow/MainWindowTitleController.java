package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationTerminator;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.cryptomator.ui.traymenu.TrayMenuComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

@MainWindowScoped
public class MainWindowTitleController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowTitleController.class);

	private final Stage window;
	private final FxApplicationTerminator terminator;
	private final FxApplicationWindows appWindows;
	private final boolean trayMenuInitialized;
	private final UpdateChecker updateChecker;
	private final ObservableValue<Boolean> updateAvailable;
	private final LicenseHolder licenseHolder;
	private final Settings settings;
	private final BooleanBinding showMinimizeButton;

	public HBox titleBar;
	private double xOffset;
	private double yOffset;

	@Inject
	MainWindowTitleController(@MainWindow Stage window, FxApplicationTerminator terminator, FxApplicationWindows appWindows, TrayMenuComponent trayMenu, UpdateChecker updateChecker, LicenseHolder licenseHolder, Settings settings) {
		this.window = window;
		this.terminator = terminator;
		this.appWindows = appWindows;
		this.trayMenuInitialized = trayMenu.isInitialized();
		this.updateChecker = updateChecker;
		this.updateAvailable = updateChecker.updateAvailableProperty();
		this.licenseHolder = licenseHolder;
		this.settings = settings;
		this.showMinimizeButton = Bindings.createBooleanBinding(this::isShowMinimizeButton, settings.showMinimizeButton, settings.showTrayIcon);
	}

	@FXML
	public void initialize() {
		LOG.trace("init MainWindowTitleController");
		updateChecker.automaticallyCheckForUpdatesIfEnabled();
		titleBar.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();

		});
		titleBar.setOnMouseClicked(event -> {
			if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
				window.setFullScreen(!window.isFullScreen());
			}
		});
		titleBar.setOnMouseDragged(event -> {
			if (window.isFullScreen()) return;
			window.setX(event.getScreenX() - xOffset);
			window.setY(event.getScreenY() - yOffset);
		});
		titleBar.setOnDragDetected(mouseDragEvent -> {
			titleBar.startFullDrag();
		});
		titleBar.setOnMouseDragReleased(mouseDragEvent -> {
			saveWindowSettings();
		});

		window.setOnCloseRequest(event -> {
			close();
			event.consume();
		});
	}

	private void saveWindowSettings() {
		settings.windowXPosition.setValue(window.getX());
		settings.windowYPosition.setValue(window.getY());
		settings.windowWidth.setValue(window.getWidth());
		settings.windowHeight.setValue(window.getHeight());
	}

	@FXML
	public void close() {
		if (trayMenuInitialized) {
			window.close();
		} else {
			terminator.terminate();
		}
	}

	@FXML
	public void minimize() {
		window.setIconified(true);
	}

	@FXML
	public void showPreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.ANY);
	}

	@FXML
	public void showGeneralPreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.GENERAL);
	}

	@FXML
	public void showContributePreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.CONTRIBUTE);
	}

	/* Getter/Setter */

	public LicenseHolder getLicenseHolder() {
		return licenseHolder;
	}

	public ObservableValue<Boolean> updateAvailableProperty() {
		return updateAvailable;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable.getValue();
	}

	public boolean isTrayIconPresent() {
		return trayMenuInitialized;
	}

	public ReadOnlyBooleanProperty debugModeEnabledProperty() {
		return settings.debugMode;
	}

	public boolean isDebugModeEnabled() {
		return debugModeEnabledProperty().get();
	}

	public BooleanBinding showMinimizeButtonProperty() {
		return showMinimizeButton;
	}

	public boolean isShowMinimizeButton() {
		// always show the minimize button if no tray icon is present OR it is explicitly enabled
		return !trayMenuInitialized || settings.showMinimizeButton.get();
	}
}

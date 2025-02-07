package org.cryptomator.ui.mainwindow;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

@MainWindowScoped
public class MainWindowController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

	private final Stage window;
	private final ReadOnlyObjectProperty<Vault> selectedVault;
	private final Settings settings;
	private final FxApplicationWindows appWindows;
	private final BooleanBinding updateAvailable;
	private final LicenseHolder licenseHolder;

	@FXML
	private StackPane root;

	@Inject
	public MainWindowController(@MainWindow Stage window, //
								ObjectProperty<Vault> selectedVault, //
								Settings settings, //
								FxApplicationWindows appWindows, //
								UpdateChecker updateChecker, //
								LicenseHolder licenseHolder) {
		this.window = window;
		this.selectedVault = selectedVault;
		this.settings = settings;
		this.appWindows = appWindows;
		this.updateAvailable = updateChecker.updateAvailableProperty();
		this.licenseHolder = licenseHolder;
		updateChecker.automaticallyCheckForUpdatesIfEnabled();

	}

	@FXML
	public void initialize() {
		LOG.trace("init MainWindowController");

		if (SystemUtils.IS_OS_WINDOWS) {
			root.getStyleClass().add("os-windows");
		}
		window.focusedProperty().addListener(this::mainWindowFocusChanged);

		int x = settings.windowXPosition.get();
		int y = settings.windowYPosition.get();
		int width = settings.windowWidth.get();
		int height = settings.windowHeight.get();
		if (windowPositionSaved(x, y, width, height) && isWithinDisplayBounds(x, y, width, height)) {
			window.setX(x);
			window.setY(y);
			window.setWidth(width);
			window.setHeight(height);
		}
		settings.windowXPosition.bind(window.xProperty());
		settings.windowYPosition.bind(window.yProperty());
		settings.windowWidth.bind(window.widthProperty());
		settings.windowHeight.bind(window.heightProperty());
	}

	private boolean windowPositionSaved(int x, int y, int width, int height) {
		return x != 0 || y != 0 || width != 0 || height != 0;
	}

	private boolean isWithinDisplayBounds(int x, int y, int width, int height) {
		// define a rect which is inset on all sides from the window's rect:
		final double slackedX = x + 20; // 20px left
		final double slackedY = y + 5; // 5px top
		final double slackedWidth = width - 40; // 20px left + 20px right
		final double slackedHeight = height - 25; // 5px top + 20px bottom
		return isRectangleWithinBounds(slackedX, slackedY, 0, slackedHeight) // Left pixel column
				&& isRectangleWithinBounds(slackedX + slackedWidth, slackedY, 0, slackedHeight) // Right pixel column
				&& isRectangleWithinBounds(slackedX, slackedY, slackedWidth, 0) // Top pixel row
				&& isRectangleWithinBounds(slackedX, slackedY + slackedHeight, slackedWidth, 0); // Bottom pixel row
	}

	private boolean isRectangleWithinBounds(double x, double y, double width, double height) {
		return !Screen.getScreensForRectangle(x, y, width, height).isEmpty();
	}

	private void mainWindowFocusChanged(Observable observable) {
		var v = selectedVault.get();
		if (v != null) {
			VaultListManager.redetermineVaultState(v);
		}
	}

	@FXML
	public void showGeneralPreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.GENERAL);
	}

	@FXML
	public void showContributePreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.CONTRIBUTE);
	}

	@FXML
	public void showUpdatePreferences() {
		appWindows.showPreferencesWindow(SelectedPreferencesTab.UPDATES);
	}

	public ReadOnlyBooleanProperty debugModeEnabledProperty() {
		return settings.debugMode;
	}

	public boolean getDebugModeEnabled() {
		return debugModeEnabledProperty().get();
	}

	public BooleanBinding updateAvailableProperty() {
		return updateAvailable;
	}

	public boolean getUpdateAvailable() {
		return updateAvailable.get();
	}

	public BooleanBinding licenseValidProperty(){
		return licenseHolder.validLicenseProperty();
	}

	public boolean getLicenseValid() {
		return licenseHolder.isValidLicense();
	}

}

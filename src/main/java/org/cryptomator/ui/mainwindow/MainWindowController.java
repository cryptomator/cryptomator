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
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

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
		if (windowPositionSaved(x, y, width, height)) {
			window.setX(x);
			window.setY(y);
			window.setWidth(Math.clamp(width, window.getMinWidth(), window.getMaxWidth()));
			window.setHeight(Math.clamp(height, window.getMinHeight(), window.getMaxHeight()));
		}

		window.setOnShowing(this::checkDisplayBounds);

		settings.windowXPosition.bind(window.xProperty());
		settings.windowYPosition.bind(window.yProperty());
		settings.windowWidth.bind(window.widthProperty());
		settings.windowHeight.bind(window.heightProperty());
	}

	private boolean windowPositionSaved(int x, int y, int width, int height) {
		return x != 0 || y != 0 || width != 0 || height != 0;
	}

	private void checkDisplayBounds(WindowEvent windowEvent) {
		int x = settings.windowXPosition.get();
		int y = settings.windowYPosition.get();
		int width = settings.windowWidth.get();
		int height = settings.windowHeight.get();

		Rectangle2D primaryScreenBounds = Screen.getPrimary().getBounds();
		if (!isWithinDisplayBounds(x, y, width, height)) { //use stored window position
			LOG.debug("Resetting window position due to insufficient screen overlap");
			var centeredX = (primaryScreenBounds.getWidth() - window.getMinWidth()) / 2;
			var centeredY = (primaryScreenBounds.getHeight() - window.getMinHeight()) / 2;
			//check if we can keep width and height
			if (isWithinDisplayBounds((int) centeredX, (int) centeredY, width, height)) {
				//if so, keep window size
				window.setWidth(Math.clamp(width, window.getMinWidth(), window.getMaxWidth()));
				window.setHeight(Math.clamp(height, window.getMinHeight(), window.getMaxHeight()));
			}
			//reset position of upper left corner
			window.setX(centeredX);
			window.setY(centeredY);
		}
	}

	private boolean isWithinDisplayBounds(int x, int y, int width, int height) {
		// define a rect which is inset on all sides from the window's rect:
		final int shrinkedX = x + 20; // 20px left
		final int shrinkedY = y + 5; // 5px top
		final int shrinkedWidth = width - 40; // 20px left + 20px right
		final int shrinkedHeigth = height - 25; // 5px top + 20px bottom
		return isRectangleWithinBounds(shrinkedX, shrinkedY, 0, shrinkedHeigth) // Left pixel column
				&& isRectangleWithinBounds(shrinkedX + shrinkedWidth, shrinkedY, 0, shrinkedHeigth) // Right pixel column
				&& isRectangleWithinBounds(shrinkedX, shrinkedY, shrinkedWidth, 0) // Top pixel row
				&& isRectangleWithinBounds(shrinkedX, shrinkedY + shrinkedHeigth, shrinkedWidth, 0); // Bottom pixel row
	}

	private boolean isRectangleWithinBounds(int x, int y, int width, int height) {
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

	public BooleanBinding licenseValidProperty() {
		return licenseHolder.validLicenseProperty();
	}

	public boolean getLicenseValid() {
		return licenseHolder.isValidLicense();
	}

}

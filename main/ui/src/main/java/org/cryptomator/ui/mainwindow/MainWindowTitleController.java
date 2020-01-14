package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.cryptomator.ui.preferences.SelectedPreferencesTab;
import org.cryptomator.ui.wrongfilealert.WrongFileAlertComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.awt.desktop.QuitResponse;
import java.util.concurrent.CountDownLatch;

@MainWindowScoped
public class MainWindowTitleController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowTitleController.class);

	public HBox titleBar;

	private final Stage window;
	private final FxApplication application;
	private final CountDownLatch shutdownLatch;
	private final boolean minimizeToSysTray;
	private final UpdateChecker updateChecker;
	private final BooleanBinding updateAvailable;
	private final LicenseHolder licenseHolder;

	private double xOffset;
	private double yOffset;

	@Inject
	MainWindowTitleController(@MainWindow Stage window, FxApplication application, @Named("shutdownLatch") CountDownLatch shutdownLatch, @Named("trayMenuSupported") boolean minimizeToSysTray, UpdateChecker updateChecker, LicenseHolder licenseHolder) {
		this.window = window;
		this.application = application;
		this.shutdownLatch = shutdownLatch;
		this.minimizeToSysTray = minimizeToSysTray;
		this.updateChecker = updateChecker;
		this.updateAvailable = updateChecker.latestVersionProperty().isNotNull();
		this.licenseHolder = licenseHolder;
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
	}

	@FXML
	public void close() {
		if (minimizeToSysTray) {
			window.close();
		} else {
			quitApplication();
		}
	}

	private void quitApplication() {
		application.showQuitWindow(new QuitResponse() {
			@Override
			public void performQuit() {
				shutdownLatch.countDown();
			}

			@Override
			public void cancelQuit() {
				// no-op
			}
		});
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
}

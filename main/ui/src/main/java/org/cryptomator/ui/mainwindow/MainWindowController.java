package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CountDownLatch;

@MainWindowScoped
public class MainWindowController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

	private final Stage window;
	private final FxApplication application;
	private final boolean minimizeToSysTray;
	private final UpdateChecker updateChecker;
	private final BooleanBinding updateAvailable;
	public HBox titleBar;
	public Region resizer;
	private double xOffset;
	private double yOffset;

	@Inject
	public MainWindowController(@MainWindow Stage window, FxApplication application, @Named("trayMenuSupported") boolean minimizeToSysTray, UpdateChecker updateChecker) {
		this.window = window;
		this.application = application;
		this.minimizeToSysTray = minimizeToSysTray;
		this.updateChecker = updateChecker;
		this.updateAvailable = updateChecker.latestVersionProperty().isNotNull();
	}

	@FXML
	public void initialize() {
		LOG.debug("init MainWindowController");
		titleBar.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
		});
		titleBar.setOnMouseDragged(event -> {
			window.setX(event.getScreenX() - xOffset);
			window.setY(event.getScreenY() - yOffset);
		});
		resizer.setOnMouseDragged(event -> {
			// we know for a fact that window is borderless. i.e. the scene starts at 0/0 of the window.
			window.setWidth(event.getSceneX());
			window.setHeight(event.getSceneY());
		});
		updateChecker.automaticallyCheckForUpdatesIfEnabled();
	}

	@FXML
	public void close() {
		if (minimizeToSysTray) {
			window.close();
		} else {
			window.setIconified(true);
		}
	}

	@FXML
	public void showPreferences() {
		application.showPreferencesWindow();
	}

	/* Getter/Setter */

	public BooleanBinding updateAvailableProperty() {
		return updateAvailable;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable.get();
	}
}

package org.cryptomator.ui.mainwindow;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.cryptomator.ui.FxApplication;
import org.cryptomator.ui.FxApplicationScoped;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.CountDownLatch;

@MainWindowScoped
public class MainWindowController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

	private final CountDownLatch shutdownLatch;
	private final Stage window;
	private final FxApplication application;

	@FXML
	public HBox titleBar;

	private double xOffset;
	private double yOffset;

	@Inject
	public MainWindowController(@Named("shutdownLatch") CountDownLatch shutdownLatch, Stage window, FxApplication application) {
		this.shutdownLatch = shutdownLatch;
		this.window = window;
		this.application = application;
	}

	@FXML
	public void initialize() {
		LOG.debug("init MainWindowController");
		titleBar.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
		});
		titleBar.setOnMouseDragged(event -> {
			titleBar.getScene().getWindow().setX(event.getScreenX() - xOffset);
			titleBar.getScene().getWindow().setY(event.getScreenY() - yOffset);
		});
	}

	@FXML
	public void close() {
		window.close();
		LOG.info("closed...");
		shutdownLatch.countDown();
	}

	@FXML
	public void showPreferences() {
		application.showPreferencesWindow();
	}
}

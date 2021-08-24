package org.cryptomator.ui.mainwindow;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@MainWindow
public class ResizeController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ResizeController.class);

	private final Stage window;

	public Region tlResizer;
	public Region trResizer;
	public Region blResizer;
	public Region brResizer;

	private double origX, origY, origW, origH;

	private final Settings settings;

	@Inject
	ResizeController(@MainWindow Stage window, Settings settings) {
		this.window = window;
		this.settings = settings;
	}

	@FXML
	public void initialize() {
		LOG.debug("init ResizeController");
		tlResizer.setOnMousePressed(this::startResize);
		trResizer.setOnMousePressed(this::startResize);
		blResizer.setOnMousePressed(this::startResize);
		brResizer.setOnMousePressed(this::startResize);
		tlResizer.setOnMouseDragged(this::resizeTopLeft);
		trResizer.setOnMouseDragged(this::resizeTopRight);
		blResizer.setOnMouseDragged(this::resizeBottomLeft);
		brResizer.setOnMouseDragged(this::resizeBottomRight);

		window.setY(settings.windowYPositionProperty().get());
		window.setX(settings.windowXPositionProperty().get());
		window.setHeight(settings.windowHeightProperty().get());
		window.setWidth(settings.windowWidthProperty().get());
	}

	private void startResize(MouseEvent evt) {
		origX = window.getX();
		origY = window.getY();
		origW = window.getWidth();
		origH = window.getHeight();
	}

	private void resizeTopLeft(MouseEvent evt) {
		resizeTop(evt);
		resizeLeft(evt);
		saveSettings();
	}

	private void resizeTopRight(MouseEvent evt) {
		resizeTop(evt);
		resizeRight(evt);
		saveSettings();
	}

	private void resizeBottomLeft(MouseEvent evt) {
		resizeBottom(evt);
		resizeLeft(evt);
		saveSettings();
	}

	private void resizeBottomRight(MouseEvent evt) {
		resizeBottom(evt);
		resizeRight(evt);
		saveSettings();
	}

	private void resizeTop(MouseEvent evt) {
		double newY = evt.getScreenY();
		double dy = newY - origY;
		double newH = origH - dy;
		if (newH < window.getMaxHeight() && newH > window.getMinHeight()) {
			window.setY(newY);
			window.setHeight(newH);
		}
	}

	private void resizeLeft(MouseEvent evt) {
		double newX = evt.getScreenX();
		double dx = newX - origX;
		double newW = origW - dx;
		if (newW < window.getMaxWidth() && newW > window.getMinWidth()) {
			window.setX(newX);
			window.setWidth(newW);
		}
	}

	private void resizeBottom(MouseEvent evt) {
		double newH = evt.getSceneY();
		if (newH < window.getMaxHeight() && newH > window.getMinHeight()) {
			window.setHeight(newH);
		}
	}

	private void resizeRight(MouseEvent evt) {
		double newW = evt.getSceneX();
		if (newW < window.getMaxWidth() && newW > window.getMinWidth()) {
			window.setWidth(newW);
		}
	}

	private void saveSettings() {
		settings.windowHeightProperty().setValue(window.getHeight());
		settings.windowWidthProperty().setValue(window.getWidth());
		settings.windowYPositionProperty().setValue(window.getY());
		settings.windowXPositionProperty().setValue(window.getX());
	}

}
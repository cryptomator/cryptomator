package org.cryptomator.ui.mainwindow;

import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

@MainWindow
public class ResizeController implements FxController {

	private final Stage window;

	public Region tlResizer;
	public Region trResizer;
	public Region blResizer;
	public Region brResizer;

	private double origX, origY, origW, origH;

	@Inject
	ResizeController(@MainWindow Stage window) {
		this.window = window;
		// TODO inject settings and save current position and size
	}

	@FXML
	public void initialize() {
		tlResizer.setOnMousePressed(this::startResize);
		trResizer.setOnMousePressed(this::startResize);
		blResizer.setOnMousePressed(this::startResize);
		brResizer.setOnMousePressed(this::startResize);
		tlResizer.setOnMouseDragged(this::resizeTopLeft);
		trResizer.setOnMouseDragged(this::resizeTopRight);
		blResizer.setOnMouseDragged(this::resizeBottomLeft);
		brResizer.setOnMouseDragged(this::resizeBottomRight);
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
	}

	private void resizeTopRight(MouseEvent evt) {
		resizeTop(evt);
		resizeRight(evt);
	}

	private void resizeBottomLeft(MouseEvent evt) {
		resizeBottom(evt);
		resizeLeft(evt);
	}

	private void resizeBottomRight(MouseEvent evt) {
		resizeBottom(evt);
		resizeRight(evt);
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

}

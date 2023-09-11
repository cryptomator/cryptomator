package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@MainWindow
public class ResizeController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ResizeController.class);

	private final Stage window;

	public Region tlResizer;
	public Region trResizer;
	public Region blResizer;
	public Region brResizer;
	public Region tResizer;
	public Region rResizer;
	public Region bResizer;
	public Region lResizer;
	public Region lDefaultRegion;
	public Region tDefaultRegion;
	public Region rDefaultRegion;
	public Region bDefaultRegion;

	private double origX, origY, origW, origH;

	private final Settings settings;

	private final BooleanBinding showResizingArrows;

	@Inject
	ResizeController(@MainWindow Stage window, Settings settings) {
		this.window = window;
		this.settings = settings;
		this.showResizingArrows = window.fullScreenProperty().not();
	}

	@FXML
	public void initialize() {
		LOG.trace("init ResizeController");

		if (!neverTouched()) {
			window.setHeight(settings.windowHeight.get() > window.getMinHeight() ? settings.windowHeight.get() : window.getMinHeight());
			window.setWidth(settings.windowWidth.get() > window.getMinWidth() ? settings.windowWidth.get() : window.getMinWidth());
			window.setX(settings.windowXPosition.get());
			window.setY(settings.windowYPosition.get());
		}

		window.setOnShowing(this::checkDisplayBounds);
	}

	private boolean neverTouched() {
		return (settings.windowHeight.get() == 0) && (settings.windowWidth.get() == 0) && (settings.windowXPosition.get() == 0) && (settings.windowYPosition.get() == 0);
	}

	private boolean isWithinDisplayBounds() {
		// (x1, y1) is the top left corner of the window, (x2, y2) is the bottom right corner
		final double slack = 10;
		final double width = window.getWidth() - 2 * slack;
		final double height = window.getHeight() - 2 * slack;
		final double x1 = window.getX() + slack;
		final double y1 = window.getY() + slack;
		final double x2 = x1 + width;
		final double y2 = y1 + height;

		final ObservableList<Screen> screens = Screen.getScreensForRectangle(x1, y1, width, height);

		// Find the total visible area of the window
		double visibleArea = 0;
		for (Screen screen : screens) {
			Rectangle2D bounds = screen.getVisualBounds();

			double xOverlap = Math.min(x2, bounds.getMaxX()) - Math.max(x1, bounds.getMinX());
			double yOverlap = Math.min(y2, bounds.getMaxY()) - Math.max(y1, bounds.getMinY());

			visibleArea += xOverlap * yOverlap;
		}

		final double windowArea = width * height;

		// Within bounds if the visible area matches the window area
		return visibleArea == windowArea;
	}

	private void checkDisplayBounds(WindowEvent evt) {

		// Minimizing a window in Windows and closing it could result in an out of bounds position at (x, y) = (-32000, -32000)
		// See https://devblogs.microsoft.com/oldnewthing/20041028-00/?p=37453
		// If the position is (-32000, -32000), restore to the last saved position
		if (window.getX() == -32000 && window.getY() == -32000) {
			window.setX(settings.windowXPosition.get());
			window.setY(settings.windowYPosition.get());
			window.setWidth(settings.windowWidth.get());
			window.setHeight(settings.windowHeight.get());
		}

		if (!isWithinDisplayBounds()) {
			// If the position is illegal, then the window appears on the main screen in the middle of the window.
			Rectangle2D primaryScreenBounds = Screen.getPrimary().getBounds();
			window.setX((primaryScreenBounds.getWidth() - window.getMinWidth()) / 2);
			window.setY((primaryScreenBounds.getHeight() - window.getMinHeight()) / 2);
			window.setWidth(window.getMinWidth());
			window.setHeight(window.getMinHeight());
			savePositionalSettings();
		}
	}

	private void startResize(MouseEvent evt) {
		origX = window.getX();
		origY = window.getY();
		origW = window.getWidth();
		origH = window.getHeight();
	}

	@FXML
	private void resizeTopLeft(MouseEvent evt) {
		resizeTop(evt);
		resizeLeft(evt);
	}

	@FXML
	private void resizeTopRight(MouseEvent evt) {
		resizeTop(evt);
		resizeRight(evt);
	}

	@FXML
	private void resizeBottomLeft(MouseEvent evt) {
		resizeBottom(evt);
		resizeLeft(evt);
	}

	@FXML
	private void resizeBottomRight(MouseEvent evt) {
		resizeBottom(evt);
		resizeRight(evt);
	}

	@FXML
	private void resizeTop(MouseEvent evt) {
		startResize(evt);
		double newY = evt.getScreenY();
		double dy = newY - origY;
		double newH = origH - dy;
		if (newH < window.getMaxHeight() && newH > window.getMinHeight()) {
			window.setY(newY);
			window.setHeight(newH);
		}
	}

	@FXML
	private void resizeLeft(MouseEvent evt) {
		startResize(evt);
		double newX = evt.getScreenX();
		double dx = newX - origX;
		double newW = origW - dx;
		if (newW < window.getMaxWidth() && newW > window.getMinWidth()) {
			window.setX(newX);
			window.setWidth(newW);
		}
	}

	@FXML
	private void resizeBottom(MouseEvent evt) {
		double newH = evt.getSceneY();
		if (newH < window.getMaxHeight() && newH > window.getMinHeight()) {
			window.setHeight(newH);
		}
	}

	@FXML
	private void resizeRight(MouseEvent evt) {
		double newW = evt.getSceneX();
		if (newW < window.getMaxWidth() && newW > window.getMinWidth()) {
			window.setWidth(newW);
		}
	}

	@FXML
	public void savePositionalSettings() {
		settings.windowWidth.setValue(window.getWidth());
		settings.windowHeight.setValue(window.getHeight());
		settings.windowXPosition.setValue(window.getX());
		settings.windowYPosition.setValue(window.getY());
	}

	public BooleanBinding showResizingArrowsProperty() {
		return showResizingArrows;
	}

	public boolean isShowResizingArrows() {
		return showResizingArrows.get();
	}
}
package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.*;

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

	private double origX, origY, origW, origH;

	private final Settings settings;

	private final BooleanBinding showResizingArrows;

	@Inject
	ResizeController(@MainWindow Stage window, Settings settings) {
		this.window = window;
		this.settings = settings;
		this.showResizingArrows = Bindings.createBooleanBinding(this::isShowResizingArrows, window.fullScreenProperty());
	}

	@FXML
	public void initialize() {
		LOG.trace("init ResizeController");

		if (neverTouched()){
			settings.displayConfigurationProperty().setValue(getMonitorSizes());
			return;
		} else {
			if (checkForChangedDisplayConfiguration()) {
				//If the position is illegal, then the window appears on the main screen in the middle of the window.
				Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
				window.setX((size.getWidth() - window.getMinWidth()) / 2);
				window.setY((size.getHeight() - window.getMinHeight()) / 2);
				window.setWidth(window.getMinWidth());
				window.setHeight(window.getMinHeight());
			}
			else {
				window.setHeight(settings.windowHeightProperty().get() > window.getMinHeight() ? settings.windowHeightProperty().get() : window.getMinHeight());
				window.setWidth(settings.windowWidthProperty().get() > window.getMinWidth() ? settings.windowWidthProperty().get() : window.getMinWidth());
				window.setX(settings.windowXPositionProperty().get());
				window.setY(settings.windowYPositionProperty().get());
			}
		}
		savePositionalSettings();
	}

	private boolean neverTouched(){
		return (settings.windowHeightProperty().get() == 0) && (settings.windowWidthProperty().get() == 0) &&
				(settings.windowXPositionProperty().get() == 0) && (settings.windowYPositionProperty().get() == 0);
	}

	private boolean checkForChangedDisplayConfiguration(){
		String currentDisplayConfiguration = getMonitorSizes();
		String settingsDisplayConfiguration = settings.displayConfigurationProperty().get();
		boolean changedConfiguration = !settingsDisplayConfiguration.equals(currentDisplayConfiguration);
		if (changedConfiguration) settings.displayConfigurationProperty().setValue(currentDisplayConfiguration);
		return changedConfiguration;
	}

	private String getMonitorSizes() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[]    gs = ge.getScreenDevices();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < gs.length; i++) {
			DisplayMode dm = gs[i].getDisplayMode();
			if(!sb.isEmpty()) sb.append(" ");
			sb.append("displayId: " + i + ", " + dm.getWidth() + "x" + dm.getHeight() + ";");
		}
		return sb.toString();
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
		savePositionalSettings();
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
		savePositionalSettings();
	}

	@FXML
	private void resizeBottom(MouseEvent evt) {
		double newH = evt.getSceneY();
		if (newH < window.getMaxHeight() && newH > window.getMinHeight()) {
			window.setHeight(newH);
		}
		savePositionalSettings();
	}

	@FXML
	private void resizeRight(MouseEvent evt) {
		double newW = evt.getSceneX();
		if (newW < window.getMaxWidth() && newW > window.getMinWidth()) {
			window.setWidth(newW);
		}
		savePositionalSettings();
	}

	private void savePositionalSettings() {
		settings.windowHeightProperty().setValue(window.getHeight());
		settings.windowWidthProperty().setValue(window.getWidth());
		settings.windowYPositionProperty().setValue(window.getY());
		settings.windowXPositionProperty().setValue(window.getX());
	}

	public BooleanBinding showResizingArrowsProperty() {
		return showResizingArrows;
	}

	public boolean isShowResizingArrows() {
		//If in fullscreen resizing is not be possible;
		return !window.isFullScreen();
	}

}
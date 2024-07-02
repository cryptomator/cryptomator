package org.cryptomator.ui.mainwindow;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

@MainWindowScoped
public class MainWindowController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

	private final Stage window;
	private final ReadOnlyObjectProperty<Vault> selectedVault;
	private final Settings settings;

	public StackPane root;

	@Inject
	public MainWindowController(@MainWindow Stage window, ObjectProperty<Vault> selectedVault, Settings settings) {
		this.window = window;
		this.selectedVault = selectedVault;
		this.settings = settings;
	}

	@FXML
	public void initialize() {
		LOG.trace("init MainWindowController");
		if (SystemUtils.IS_OS_WINDOWS) {
			root.getStyleClass().add("os-windows");
		}
		window.focusedProperty().addListener(this::mainWindowFocusChanged);

		if (!neverTouched()) {
			window.setHeight(settings.windowHeight.get() > window.getMinHeight() ? settings.windowHeight.get() : window.getMinHeight());
			window.setWidth(settings.windowWidth.get() > window.getMinWidth() ? settings.windowWidth.get() : window.getMinWidth());
			window.setX(settings.windowXPosition.get());
			window.setY(settings.windowYPosition.get());
		}
		window.widthProperty().addListener((_,_,_) -> savePositionalSettings());
		window.heightProperty().addListener((_,_,_) -> savePositionalSettings());
		window.xProperty().addListener((_,_,_) -> savePositionalSettings());
		window.yProperty().addListener((_,_,_) -> savePositionalSettings());
	}

	private boolean neverTouched() {
		return (settings.windowHeight.get() == 0) && (settings.windowWidth.get() == 0) && (settings.windowXPosition.get() == 0) && (settings.windowYPosition.get() == 0);
	}

	@FXML
	public void savePositionalSettings() {
		settings.windowWidth.setValue(window.getWidth());
		settings.windowHeight.setValue(window.getHeight());
		settings.windowXPosition.setValue(window.getX());
		settings.windowYPosition.setValue(window.getY());
	}

	private void mainWindowFocusChanged(Observable observable) {
		var v = selectedVault.get();
		if (v != null) {
			VaultListManager.redetermineVaultState(v);
		}
	}

}

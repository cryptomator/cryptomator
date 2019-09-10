package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultFactory;
import org.cryptomator.ui.common.FontLoader;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.cryptomator.ui.wrongfilealert.WrongFileAlertComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;

@MainWindowScoped
public class MainWindowController implements FxController {

	private static final String TITLE_FONT = "/css/dosis-bold.ttf";
	private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

	private final Stage window;
	private final FxApplication application;
	private final boolean minimizeToSysTray;
	private final UpdateChecker updateChecker;
	private final BooleanBinding updateAvailable;
	private final ObservableList<Vault> vaults;
	private final VaultFactory vaultFactory;
	private final WrongFileAlertComponent.Builder wrongFileAlert;
	public HBox titleBar;
	public VBox root;
	public Pane dragAndDropIndicator;
	public Region resizer;
	private double xOffset;
	private double yOffset;

	@Inject
	public MainWindowController(@MainWindow Stage window, FxApplication application, @Named("trayMenuSupported") boolean minimizeToSysTray, UpdateChecker updateChecker, ObservableList<Vault> vaults, VaultFactory vaultFactory, WrongFileAlertComponent.Builder wrongFileAlert) {
		this.window = window;
		this.application = application;
		this.minimizeToSysTray = minimizeToSysTray;
		this.updateChecker = updateChecker;
		this.updateAvailable = updateChecker.latestVersionProperty().isNotNull();
		this.vaults = vaults;
		this.vaultFactory = vaultFactory;
		this.wrongFileAlert = wrongFileAlert;
	}

	@FXML
	public void initialize() {
		LOG.debug("init MainWindowController");
		loadFont(TITLE_FONT);
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
		dragAndDropIndicator.setVisible(false);
		root.setOnDragOver(event -> {
			if (event.getGestureSource() != root && event.getDragboard().hasFiles()) {
				/* allow for both copying and moving, whatever user chooses */
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				dragAndDropIndicator.setVisible(true);
			}
			event.consume();
		});
		root.setOnDragExited(event -> dragAndDropIndicator.setVisible(false));
		root.setOnDragDropped(event -> {
			if (event.getGestureSource() != root && event.getDragboard().hasFiles()) {
				/* allow for both copying and moving, whatever user chooses */
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				File dropped = event.getDragboard().getFiles().get(0);
				if (dropped.getName().endsWith(".cryptomator")) {
					addVault(dropped);
				} else {
					wrongFileAlert.build().showWrongFileAlertWindow();
				}
			}
			event.consume();
		});
	}

	private void addVault(final File dropped) {
		if (dropped != null) {
			VaultSettings vaultSettings = VaultSettings.withRandomId();
			vaultSettings.path().setValue(dropped.toPath().toAbsolutePath().getParent());
			Vault newVault = vaultFactory.get(vaultSettings);
			vaults.add(newVault);
			//TODO: error handling?
		}
	}

	private void loadFont(String resourcePath) {
		try {
			FontLoader.load(resourcePath);
		} catch (FontLoader.FontLoaderException e) {
			LOG.warn("Error loading font from path: " + resourcePath, e);
		}
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

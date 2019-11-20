package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@MainWindowScoped
public class MainWindowController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator"; // TODO: deduplicate constant declared in multiple classes

	private final Stage window;
	private final FxApplication application;
	private final boolean minimizeToSysTray;
	private final UpdateChecker updateChecker;
	private final BooleanBinding updateAvailable;
	private final LicenseHolder licenseHolder;
	private final VaultListManager vaultListManager;
	private final WrongFileAlertComponent.Builder wrongFileAlert;
	private final BooleanProperty draggingOver = new SimpleBooleanProperty();
	private final BooleanProperty draggingVaultOver = new SimpleBooleanProperty();
	public HBox titleBar;
	public VBox root;
	public Region resizer;
	private double xOffset;
	private double yOffset;

	@Inject
	public MainWindowController(@MainWindow Stage window, FxApplication application, @Named("trayMenuSupported") boolean minimizeToSysTray, UpdateChecker updateChecker, LicenseHolder licenseHolder, VaultListManager vaultListManager, WrongFileAlertComponent.Builder wrongFileAlert) {
		this.window = window;
		this.application = application;
		this.minimizeToSysTray = minimizeToSysTray;
		this.updateChecker = updateChecker;
		this.updateAvailable = updateChecker.latestVersionProperty().isNotNull();
		this.licenseHolder = licenseHolder;
		this.vaultListManager = vaultListManager;
		this.wrongFileAlert = wrongFileAlert;
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
		root.setOnDragEntered(this::handleDragEvent);
		root.setOnDragOver(this::handleDragEvent);
		root.setOnDragDropped(this::handleDragEvent);
		root.setOnDragExited(this::handleDragEvent);
	}

	private void handleDragEvent(DragEvent event) {
		if (DragEvent.DRAG_ENTERED.equals(event.getEventType()) && event.getGestureSource() == null) {
			draggingOver.set(true);
		} else if (DragEvent.DRAG_OVER.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			event.acceptTransferModes(TransferMode.ANY);
			draggingVaultOver.set(event.getDragboard().getFiles().stream().map(File::toPath).anyMatch(this::containsVault));
		} else if (DragEvent.DRAG_DROPPED.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			Set<Path> vaultPaths = event.getDragboard().getFiles().stream().map(File::toPath).filter(this::containsVault).collect(Collectors.toSet());
			if (vaultPaths.isEmpty()) {
				wrongFileAlert.build().showWrongFileAlertWindow();
			} else {
				vaultPaths.forEach(this::addVault);
			}
			event.setDropCompleted(!vaultPaths.isEmpty());
			event.consume();
		} else if (DragEvent.DRAG_EXITED.equals(event.getEventType())) {
			draggingOver.set(false);
			draggingVaultOver.set(false);
		}
	}

	private boolean containsVault(Path path) {
		if (path.getFileName().toString().equals(MASTERKEY_FILENAME)) {
			return true;
		} else if (Files.isDirectory(path) && Files.exists(path.resolve(MASTERKEY_FILENAME))) {
			return true;
		} else {
			return false;
		}
	}

	private void addVault(Path pathToVault) {
		try {
			if (pathToVault.getFileName().toString().equals(MASTERKEY_FILENAME)) {
				vaultListManager.add(pathToVault.getParent());
			} else {
				vaultListManager.add(pathToVault);
			}
		} catch (NoSuchFileException e) {
			LOG.debug("Not a vault: {}", pathToVault);
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

	public BooleanProperty draggingOverProperty() {
		return draggingOver;
	}

	public boolean isDraggingOver() {
		return draggingOver.get();
	}

	public BooleanProperty draggingVaultOverProperty() {
		return draggingVaultOver;
	}

	public boolean isDraggingVaultOver() {
		return draggingVaultOver.get();
	}
}

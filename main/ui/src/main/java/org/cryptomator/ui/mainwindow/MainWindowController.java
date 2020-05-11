package org.cryptomator.ui.mainwindow;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.wrongfilealert.WrongFileAlertComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@MainWindowScoped
public class MainWindowController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

	private final Stage window;
	private final VaultListManager vaultListManager;
	private final ReadOnlyObjectProperty<Vault> selectedVault;
	private final WrongFileAlertComponent.Builder wrongFileAlert;
	private final BooleanProperty draggingOver = new SimpleBooleanProperty();
	private final BooleanProperty draggingVaultOver = new SimpleBooleanProperty();
	public StackPane root;

	@Inject
	public MainWindowController(@MainWindow Stage window, VaultListManager vaultListManager, ObjectProperty<Vault> selectedVault, WrongFileAlertComponent.Builder wrongFileAlert) {
		this.window = window;
		this.vaultListManager = vaultListManager;
		this.selectedVault = selectedVault;
		this.wrongFileAlert = wrongFileAlert;
	}

	@FXML
	public void initialize() {
		LOG.debug("init MainWindowController");
		root.setOnDragEntered(this::handleDragEvent);
		root.setOnDragOver(this::handleDragEvent);
		root.setOnDragDropped(this::handleDragEvent);
		root.setOnDragExited(this::handleDragEvent);
		if (SystemUtils.IS_OS_WINDOWS) {
			root.getStyleClass().add("os-windows");
		}
		window.focusedProperty().addListener(this::mainWindowFocusChanged);
	}

	private void mainWindowFocusChanged(Observable observable) {
		var v = selectedVault.get();
		if (v != null) {
			VaultListManager.redetermineVaultState(v);
		}
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

	/* Getter/Setter */

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

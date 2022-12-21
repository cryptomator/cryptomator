package org.cryptomator.ui.mainwindow;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.DirStructure;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.wrongfilealert.WrongFileAlertComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_EXT;
import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;

@MainWindowScoped
public class MainWindowController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MainWindowController.class);

	private final Stage window;
	private final VaultListManager vaultListManager;
	private final ReadOnlyObjectProperty<Vault> selectedVault;
	private final WrongFileAlertComponent.Builder wrongFileAlert;
	private final BooleanProperty draggingOver = new SimpleBooleanProperty();
	private final BooleanProperty draggingVaultOver = new SimpleBooleanProperty();
	private final BooleanProperty draggingUnlockedVaultContentOver = new SimpleBooleanProperty();
	private final BooleanProperty draggingUnknownDragboardContentOver = new SimpleBooleanProperty();
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
		LOG.trace("init MainWindowController");
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
			draggingUnlockedVaultContentOver.set(event.getDragboard().getFiles().stream().map(File::toPath).anyMatch(this::containsUnlockedVaultContent));
			draggingUnknownDragboardContentOver.set(!draggingVaultOver.get() && !draggingUnlockedVaultContentOver.get());
		} else if (DragEvent.DRAG_DROPPED.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			Set<Path> vaultPaths = event.getDragboard().getFiles().stream().map(File::toPath).filter(this::containsVault).collect(Collectors.toSet());
			if (!vaultPaths.isEmpty()) {
				vaultPaths.forEach(this::addVault);
			}
			Set<Path> ciphertextPaths = event.getDragboard().getFiles().stream().map(File::toPath).map(this::getCiphertextPath).flatMap(Optional::stream).collect(Collectors.toSet());
			if (!ciphertextPaths.isEmpty()) {
				ciphertextPaths.forEach(this::revealPath);
			}
			if (vaultPaths.isEmpty() && ciphertextPaths.isEmpty()) {
				wrongFileAlert.build().showWrongFileAlertWindow();
			}
			event.setDropCompleted(!vaultPaths.isEmpty());
			event.consume();
		} else if (DragEvent.DRAG_EXITED.equals(event.getEventType())) {
			draggingOver.set(false);
			draggingVaultOver.set(false);
			draggingUnlockedVaultContentOver.set(false);
			draggingUnknownDragboardContentOver.set(false);
		}
	}

	private boolean containsVault(Path path) {
		try {
			if (path.getFileName().toString().endsWith(CRYPTOMATOR_FILENAME_EXT)) {
				path = path.getParent();
			}
			return CryptoFileSystemProvider.checkDirStructureForVault(path, VAULTCONFIG_FILENAME, MASTERKEY_FILENAME) != DirStructure.UNRELATED;
		} catch (IOException e) {
			return false;
		}
	}

	private void addVault(Path pathToVault) {
		try {
			if (pathToVault.getFileName().toString().endsWith(CRYPTOMATOR_FILENAME_EXT)) {
				vaultListManager.add(pathToVault.getParent());
			} else {
				vaultListManager.add(pathToVault);
			}
		} catch (IOException e) {
			LOG.debug("Not a vault: {}", pathToVault);
		}
	}

	private boolean containsUnlockedVaultContent(Path path) {
		return vaultListManager.list().stream().anyMatch(v -> path.startsWith(v.getAccessPoint()));
	}

	private Optional<Path> getCiphertextPath(Path path) {
		return vaultListManager.list().stream() //
				.filter(v -> path.startsWith(v.getAccessPoint())) //
				.findAny() //
				.map(v -> {
					try {
						var cleartextPathString = path.toString().substring(v.getAccessPoint().length());
						if (!cleartextPathString.startsWith("/")) {
							cleartextPathString = "/" + cleartextPathString;
						}
						var cleartextPath = Path.of(cleartextPathString);
						return v.getCiphertextPath(cleartextPath);
					} catch (IOException e) {
						LOG.debug("Unable to get ciphertext path from path: {}", path);
						return null;
					}
				});
	}

	private void revealPath(Path path) {
		// Unable to use `application.get().getHostServices().showDocument()` here, because it does not reveal files, only folders.
		Desktop.getDesktop().browseFileDirectory(path.toFile());
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

	public BooleanProperty draggingUnlockedVaultContentOverProperty() {
		return draggingUnlockedVaultContentOver;
	}

	public boolean isDraggingUnlockedVaultContentOver() {
		return draggingUnlockedVaultContentOver.get();
	}

	public BooleanProperty draggingUnknownDragboardContentOverProperty() {
		return draggingUnknownDragboardContentOver;
	}

	public boolean isDraggingUnknownDragboardContentOver() {
		return draggingUnknownDragboardContentOver.get();
	}
}

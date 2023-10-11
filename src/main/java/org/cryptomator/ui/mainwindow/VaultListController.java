package org.cryptomator.ui.mainwindow;

import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptofs.DirStructure;
import org.cryptomator.ui.addvaultwizard.AddVaultWizardComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.removevault.RemoveVaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_EXT;
import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;
import static org.cryptomator.common.vaults.VaultState.Value.ERROR;
import static org.cryptomator.common.vaults.VaultState.Value.LOCKED;
import static org.cryptomator.common.vaults.VaultState.Value.MISSING;
import static org.cryptomator.common.vaults.VaultState.Value.NEEDS_MIGRATION;

@MainWindowScoped
public class VaultListController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultListController.class);

	private final Stage mainWindow;
	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> selectedVault;
	private final VaultListCellFactory cellFactory;
	private final AddVaultWizardComponent.Builder addVaultWizard;
	private final BooleanBinding emptyVaultList;
	private final RemoveVaultComponent.Builder removeVaultDialogue;
	private final VaultListManager vaultListManager;
	private final BooleanProperty draggingVaultOver = new SimpleBooleanProperty();
	private final ResourceBundle resourceBundle;

	public ListView<Vault> vaultList;
	public StackPane root;
	public Button addVaultBtn;
	@FXML
	private ContextMenu addVaultContextMenu;

	@Inject
	VaultListController(@MainWindow Stage mainWindow, //
						ObservableList<Vault> vaults, //
						ObjectProperty<Vault> selectedVault, //
						VaultListCellFactory cellFactory, //
						AddVaultWizardComponent.Builder addVaultWizard, //
						RemoveVaultComponent.Builder removeVaultDialogue, //
						VaultListManager vaultListManager, //
						ResourceBundle resourceBundle) {
		this.mainWindow = mainWindow;
		this.vaults = vaults;
		this.selectedVault = selectedVault;
		this.cellFactory = cellFactory;
		this.addVaultWizard = addVaultWizard;
		this.removeVaultDialogue = removeVaultDialogue;
		this.vaultListManager = vaultListManager;
		this.resourceBundle = resourceBundle;

		this.emptyVaultList = Bindings.isEmpty(vaults);

		selectedVault.addListener(this::selectedVaultDidChange);
	}

	public void initialize() {
		vaultList.setItems(vaults);
		vaultList.setCellFactory(cellFactory);
		selectedVault.bind(vaultList.getSelectionModel().selectedItemProperty());
		vaults.addListener((ListChangeListener.Change<? extends Vault> c) -> {
			while (c.next()) {
				if (c.wasAdded()) {
					Vault anyAddedVault = c.getAddedSubList().get(0);
					vaultList.getSelectionModel().select(anyAddedVault);
				}
			}
		});
		vaultList.addEventFilter(MouseEvent.MOUSE_RELEASED, this::deselect);

		//don't show context menu when no vault selected
		vaultList.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, request -> {
			if (selectedVault.get() == null) {
				request.consume();
			}
		});

		//show removeVaultDialog on certain key press
		vaultList.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
			if (keyEvent.getCode() == KeyCode.DELETE) {
				pressedShortcutToRemoveVault();
				keyEvent.consume();
			}
		});
		if (SystemUtils.IS_OS_MAC) {
			vaultList.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
				if (keyEvent.getCode() == KeyCode.BACK_SPACE) {
					pressedShortcutToRemoveVault();
					keyEvent.consume();
				}
			});
		}

		//register vault selection shortcut to the main window
		mainWindow.addEventFilter(KeyEvent.KEY_RELEASED, keyEvent -> {
			if (keyEvent.isShortcutDown() && keyEvent.getCode().isDigitKey()) {
				vaultList.getSelectionModel().select(Integer.parseInt(keyEvent.getText()) - 1);
				keyEvent.consume();
			}
		});

		root.setOnDragEntered(this::handleDragEvent);
		root.setOnDragOver(this::handleDragEvent);
		root.setOnDragDropped(this::handleDragEvent);
		root.setOnDragExited(this::handleDragEvent);
	}

	@FXML
	private void toggleMenu() {
		if (addVaultContextMenu.isShowing()) {
			addVaultContextMenu.hide();
		} else {
			addVaultContextMenu.show(addVaultBtn, Side.BOTTOM, 0.0, 0.0);
		}
	}

	private void deselect(MouseEvent released) {
		if (released.getY() > (vaultList.getItems().size() * vaultList.fixedCellSizeProperty().get())) {
			vaultList.getSelectionModel().clearSelection();
			released.consume();
		}
	}

	private void selectedVaultDidChange(@SuppressWarnings("unused") ObservableValue<? extends Vault> observableValue, @SuppressWarnings("unused") Vault oldValue, Vault newValue) {
		if (newValue == null) {
			return;
		}
		VaultListManager.redetermineVaultState(newValue);
	}

	@FXML
	public void didClickAddNewVault() {
		addVaultWizard.build().showAddNewVaultWizard(resourceBundle);
	}

	@FXML
	public void didClickAddExistingVault() {
		addVaultWizard.build().showAddExistingVaultWizard(resourceBundle);
	}

	private void pressedShortcutToRemoveVault() {
		final var vault = selectedVault.get();
		if (vault != null && EnumSet.of(LOCKED, MISSING, ERROR, NEEDS_MIGRATION).contains(vault.getState())) {
			removeVaultDialogue.vault(vault).build().showRemoveVault();
		}
	}

	private void handleDragEvent(DragEvent event) {
		if (DragEvent.DRAG_OVER.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			draggingVaultOver.set(event.getDragboard().getFiles().stream().map(File::toPath).anyMatch(this::containsVault));
			if (draggingVaultOver.get()) {
				event.acceptTransferModes(TransferMode.ANY);
			}
		} else if (DragEvent.DRAG_DROPPED.equals(event.getEventType()) && event.getGestureSource() == null && event.getDragboard().hasFiles()) {
			Set<Path> vaultPaths = event.getDragboard().getFiles().stream().map(File::toPath).filter(this::containsVault).collect(Collectors.toSet());
			if (!vaultPaths.isEmpty()) {
				vaultPaths.forEach(this::addVault);
			}
			event.setDropCompleted(!vaultPaths.isEmpty());
			event.consume();
		} else if (DragEvent.DRAG_EXITED.equals(event.getEventType())) {
			draggingVaultOver.set(false);
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

	// Getter and Setter

	public BooleanBinding emptyVaultListProperty() {
		return emptyVaultList;
	}

	public boolean isEmptyVaultList() {
		return emptyVaultList.get();
	}

	public BooleanProperty draggingVaultOverProperty() {
		return draggingVaultOver;
	}

	public boolean isDraggingVaultOver() {
		return draggingVaultOver.get();
	}


}

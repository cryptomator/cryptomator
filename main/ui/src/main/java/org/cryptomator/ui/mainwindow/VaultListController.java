package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.addvaultwizard.AddVaultWizardComponent;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

@MainWindowScoped
public class VaultListController implements FxController {


	private final Stage mainWindow;
	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> selectedVault;
	private final VaultListCellFactory cellFactory;
	private final AddVaultWizardComponent.Builder addVaultWizard;
	private final BooleanBinding emptyVaultList;

	public ListView<Vault> vaultList;

	@Inject
	VaultListController(@MainWindow Stage mainWindow, ObservableList<Vault> vaults, ObjectProperty<Vault> selectedVault, VaultListCellFactory cellFactory, AddVaultWizardComponent.Builder addVaultWizard) {
		this.mainWindow = mainWindow;
		this.vaults = vaults;
		this.selectedVault = selectedVault;
		this.cellFactory = cellFactory;
		this.addVaultWizard = addVaultWizard;

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
		vaultList.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, request -> {
			if (selectedVault.get() == null) {
				request.consume();
			}
		});
		mainWindow.addEventFilter(KeyEvent.KEY_RELEASED, keyevent -> {
			if (keyevent.isShortcutDown() && keyevent.getCode().isDigitKey()) {
				vaultList.getSelectionModel().select(Integer.parseInt(keyevent.getText()) - 1);
			}
		});
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
	public void didClickAddVault() {
		addVaultWizard.build().showAddVaultWizard();
	}

	// Getter and Setter

	public BooleanBinding emptyVaultListProperty() {
		return emptyVaultList;
	}

	public boolean isEmptyVaultList() {
		return emptyVaultList.get();
	}

}

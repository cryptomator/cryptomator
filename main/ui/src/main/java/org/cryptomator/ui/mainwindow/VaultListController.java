package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.addvaultwizard.AddVaultWizardComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.removevault.RemoveVaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

//TODO: Add check if a vault in the list is invalid and add notification & controller
@MainWindowScoped
public class VaultListController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultListController.class);

	private final Stage window;
	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> selectedVault;
	private final VaultListCellFactory cellFactory;
	private final AddVaultWizardComponent.Builder addVaultWizard;
	private final RemoveVaultComponent.Builder removeVault;
	private final BooleanBinding noVaultSelected;
	private final BooleanBinding emptyVaultList;
	public ListView<Vault> vaultList;

	@Inject
	VaultListController(@MainWindow Stage window, ObservableList<Vault> vaults, ObjectProperty<Vault> selectedVault, VaultListCellFactory cellFactory, AddVaultWizardComponent.Builder addVaultWizard, RemoveVaultComponent.Builder removeVault) {
		this.window = window;
		this.vaults = vaults;
		this.selectedVault = selectedVault;
		this.cellFactory = cellFactory;
		this.addVaultWizard = addVaultWizard;
		this.removeVault = removeVault;
		this.noVaultSelected = selectedVault.isNull();
		this.emptyVaultList = Bindings.isEmpty(vaults);
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
	}

	@FXML
	public void didClickAddVault() {
		addVaultWizard.build().showAddVaultWizard();
	}

	@FXML
	public void didClickRemoveVault() {
		Vault v = selectedVault.get();
		if (v != null) {
			removeVault.vault(v).build().showRemoveVault();
		} else {
			LOG.debug("Cannot remove a vault if none is selected.");
		}
	}

	// Getter and Setter

	public BooleanBinding emptyVaultListProperty() {
		return emptyVaultList;
	}

	public boolean isEmptyVaultList() {
		return emptyVaultList.get();
	}

	public BooleanBinding noVaultSelectedProperty() {
		return noVaultSelected;
	}

	public boolean isNoVaultSelected() {
		return noVaultSelected.get();
	}
}

package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import org.cryptomator.ui.addvaultwizard.AddVaultWizardComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.common.vaults.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@MainWindowScoped
public class VaultListController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultListController.class);

	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> selectedVault;
	private final VaultListCellFactory cellFactory;
	private final AddVaultWizardComponent.Builder addVaultWizard;
	public ListView<Vault> vaultList;
	public AnchorPane onboardingOverlay;

	@Inject
	VaultListController(ObservableList<Vault> vaults, ObjectProperty<Vault> selectedVault, VaultListCellFactory cellFactory, AddVaultWizardComponent.Builder addVaultWizard) {
		this.vaults = vaults;
		this.selectedVault = selectedVault;
		this.cellFactory = cellFactory;
		this.addVaultWizard = addVaultWizard;
	}

	public void initialize() {
		onboardingOverlay.visibleProperty().bind(Bindings.isEmpty(vaults));
		vaultList.setItems(vaults);
		vaultList.setCellFactory(cellFactory);
		selectedVault.bind(vaultList.getSelectionModel().selectedItemProperty());
	}

	public void didClickAddVault() {
		addVaultWizard.build().showAddVaultWizard();
	}

	public void didClickRemoveVault() {
		//TODO: Dialogue
		if (selectedVault.get() != null) {
			vaults.remove(selectedVault.get());
			LOG.debug("Removing vault {}.", selectedVault.get().getDisplayableName());
		} else {
			LOG.debug("Cannot remove a vault if none is selected.");
		}
	}
}

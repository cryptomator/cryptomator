package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.model.Vault;

import javax.inject.Inject;

@MainWindowScoped
public class VaultListController implements FxController {

	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> selectedVault;
	public ListView vaultList;
	public AnchorPane onboardingOverlay;

	@Inject
	public VaultListController(ObservableList<Vault> vaults, ObjectProperty<Vault> selectedVault) {
		this.vaults = vaults;
		this.selectedVault = selectedVault;
	}

	public void initialize() {
		onboardingOverlay.visibleProperty().bind(Bindings.isEmpty(vaults));
		vaultList.setItems(vaults);
		selectedVault.bind(vaultList.getSelectionModel().selectedItemProperty());
	}

	public void didClickAddVault(ActionEvent actionEvent) {
	}

	public void didClickRemoveVault(ActionEvent actionEvent) {
	}
}

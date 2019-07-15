package org.cryptomator.ui.mainwindow;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import org.cryptomator.ui.FxApplicationScoped;
import org.cryptomator.ui.FxController;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@FxApplicationScoped
public class VaultListController implements FxController {
	
	private static final Logger LOG = LoggerFactory.getLogger(VaultListController.class);

	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> selectedVault;
	public ListView vaultList;
	public AnchorPane onboardingOverlay;

	@Inject
	public VaultListController(ObservableList<Vault> vaults, ObjectProperty<Vault> selectedVault) {
		this.vaults = vaults;
		this.selectedVault = selectedVault;
	}

	@FXML
	public void initialize() {
		LOG.debug("init VaultListController");
		onboardingOverlay.visibleProperty().bind(Bindings.isEmpty(vaults));
		vaultList.setItems(vaults);
		selectedVault.bind(vaultList.getSelectionModel().selectedItemProperty());
	}

	public void didClickAddVault(ActionEvent actionEvent) {
	}

	public void didClickRemoveVault(ActionEvent actionEvent) {
	}
}

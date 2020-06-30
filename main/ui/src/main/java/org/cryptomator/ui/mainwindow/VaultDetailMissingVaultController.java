package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@MainWindowScoped
public class VaultDetailMissingVaultController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;

	@Inject
	public VaultDetailMissingVaultController(ObjectProperty<Vault> vault) {
		this.vault = vault;
	}

	@FXML
	public void recheck() {
		VaultListManager.redetermineVaultState(vault.get());
	}
}

package org.cryptomator.ui.vaultoptions;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@VaultOptionsScoped
public class GeneralVaultOptionsController implements FxController {

	private final Vault vault;
	public CheckBox unlockOnStartupCheckbox;

	@Inject
	GeneralVaultOptionsController(@VaultOptionsWindow Vault vault) {
		this.vault = vault;
	}

	@FXML
	public void initialize() {
		unlockOnStartupCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().unlockAfterStartup());
	}
}

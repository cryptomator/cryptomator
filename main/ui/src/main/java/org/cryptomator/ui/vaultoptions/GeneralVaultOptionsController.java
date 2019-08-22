package org.cryptomator.ui.vaultoptions;

import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@VaultOptionsScoped
public class GeneralVaultOptionsController implements FxController {

	private final Vault vault;
	private final ChangePasswordComponent.Builder changePasswordWindow;

	@Inject
	GeneralVaultOptionsController(@VaultOptionsWindow Vault vault, ChangePasswordComponent.Builder changePasswordWindow) {
		this.vault = vault;
		this.changePasswordWindow = changePasswordWindow;
	}

	@FXML
	public void changePassword() {
		changePasswordWindow.vault(vault).build().showChangePasswordWindow();
	}

}

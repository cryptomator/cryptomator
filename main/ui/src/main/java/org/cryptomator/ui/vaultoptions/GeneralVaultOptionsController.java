package org.cryptomator.ui.vaultoptions;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@VaultOptionsScoped
public class GeneralVaultOptionsController implements FxController {

	private final Vault vault;
	private final Stage window;
	private final ChangePasswordComponent.Builder changePasswordWindow;

	@Inject
	GeneralVaultOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ChangePasswordComponent.Builder changePasswordWindow) {
		this.vault = vault;
		this.window = window;
		this.changePasswordWindow = changePasswordWindow;
	}

	@FXML
	public void changePassword() {
		changePasswordWindow.vault(vault).owner(window).build().showChangePasswordWindow();
	}

}

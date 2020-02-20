package org.cryptomator.ui.vaultoptions;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;

import javax.inject.Inject;

@VaultOptionsScoped
public class MasterkeyOptionsController implements FxController {

	private final Vault vault;
	private final Stage window;
	private final ChangePasswordComponent.Builder changePasswordWindow;
	private final RecoveryKeyComponent.Builder recoveryKeyWindow;

	@Inject
	MasterkeyOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ChangePasswordComponent.Builder changePasswordWindow, RecoveryKeyComponent.Builder recoveryKeyWindow) {
		this.vault = vault;
		this.window = window;
		this.changePasswordWindow = changePasswordWindow;
		this.recoveryKeyWindow = recoveryKeyWindow;
	}

	@FXML
	public void changePassword() {
		changePasswordWindow.vault(vault).owner(window).build().showChangePasswordWindow();
	}

	@FXML
	public void showRecoveryKey() {
		recoveryKeyWindow.vault(vault).owner(window).build().showRecoveryKeyCreationWindow();
	}

	@FXML
	public void showRecoverVaultDialogue() {
		recoveryKeyWindow.vault(vault).owner(window).build().showRecoveryKeyRecoverWindow();
	}
}

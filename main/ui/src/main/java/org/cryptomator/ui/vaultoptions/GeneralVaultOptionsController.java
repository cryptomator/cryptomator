package org.cryptomator.ui.vaultoptions;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.recovervault.RecoverVaultComponent;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;

import javax.inject.Inject;

@VaultOptionsScoped
public class GeneralVaultOptionsController implements FxController {

	private final Vault vault;
	private final Stage window;
	private final ChangePasswordComponent.Builder changePasswordWindow;
	private final RecoveryKeyComponent.Builder recoveryKeyWindow;
	private final RecoverVaultComponent.Builder recoverVaultWindow;

	@Inject
	GeneralVaultOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ChangePasswordComponent.Builder changePasswordWindow, RecoveryKeyComponent.Builder recoveryKeyWindow, RecoverVaultComponent.Builder recoverVaultWindow) {
		this.vault = vault;
		this.window = window;
		this.changePasswordWindow = changePasswordWindow;
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.recoverVaultWindow = recoverVaultWindow;
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
	public void showRecoverVaultDialogue(){
		recoverVaultWindow.vault(vault).owner(window).build().showRecoverVaultWindow();
	}

}

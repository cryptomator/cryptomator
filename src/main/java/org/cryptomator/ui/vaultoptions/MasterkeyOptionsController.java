package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.forgetpassword.ForgetPasswordComponent;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

@VaultOptionsScoped
public class MasterkeyOptionsController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MasterkeyOptionsController.class);

	private final Vault vault;
	private final Stage window;
	private final ChangePasswordComponent.Builder changePasswordWindow;
	private final RecoveryKeyComponent.Factory recoveryKeyWindow;
	private final ForgetPasswordComponent.Builder forgetPasswordWindow;
	public CheckBox useTouchIDheckbox;
	private final KeychainManager keychain;
	private final ObservableValue<Boolean> passwordSaved;


	@Inject
	MasterkeyOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ChangePasswordComponent.Builder changePasswordWindow, RecoveryKeyComponent.Factory recoveryKeyWindow, ForgetPasswordComponent.Builder forgetPasswordWindow, KeychainManager keychain) {
		this.vault = vault;
		this.window = window;
		this.changePasswordWindow = changePasswordWindow;
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.forgetPasswordWindow = forgetPasswordWindow;
		this.keychain = keychain;
		if (keychain.isSupported() && !keychain.isLocked()) {
			this.passwordSaved = keychain.getPassphraseStoredProperty(vault.getId()).orElse(false);
		} else {
			this.passwordSaved = new SimpleBooleanProperty(false);
		}
	}

	@FXML
	public void changePassword() {
		changePasswordWindow.vault(vault).owner(window).build().showChangePasswordWindow();
	}

	@FXML
	public void showRecoveryKey() {
		recoveryKeyWindow.create(vault, window).showRecoveryKeyCreationWindow();
	}

	@FXML
	public void showRecoverVaultDialog() {
		recoveryKeyWindow.create(vault, window).showRecoveryKeyRecoverWindow();
	}

	@FXML
	public void showForgetPasswordDialog() {
		assert keychain.isSupported();
		forgetPasswordWindow.vault(vault).owner(window).build().showForgetPassword();
	}

	public ObservableValue<Boolean> passwordSavedProperty() {
		return passwordSaved;
	}

	public boolean isPasswordSaved() {
		return passwordSaved.getValue();
	}
}

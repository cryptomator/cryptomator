package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

@VaultOptionsScoped
public class MasterkeyOptionsController implements FxController {

	private final Vault vault;
	private final Stage window;
	private final ChangePasswordComponent.Builder changePasswordWindow;
	private final RecoveryKeyComponent.Builder recoveryKeyWindow;
	private final ForgetPasswordComponent.Builder forgetPasswordWindow;
	public CheckBox disableAllKeyringsCheckbox;
	private final KeychainManager keychain;
	private final BooleanExpression passwordSaved;


	@Inject
	MasterkeyOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ChangePasswordComponent.Builder changePasswordWindow, RecoveryKeyComponent.Builder recoveryKeyWindow, ForgetPasswordComponent.Builder forgetPasswordWindow, KeychainManager keychain) {
		this.vault = vault;
		this.window = window;
		this.changePasswordWindow = changePasswordWindow;
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.forgetPasswordWindow = forgetPasswordWindow;
		this.keychain = keychain;
		if (keychain.isSupported() && !keychain.isLocked()) {
			this.passwordSaved = Bindings.createBooleanBinding(this::isPasswordSaved, keychain.getPassphraseStoredProperty(vault.getId()));
		} else {
			this.passwordSaved = new SimpleBooleanProperty(false);
		}
	}

	@FXML
	public void initialize() {
		this.disableAllKeyringsCheckbox.selectedProperty().bindBidirectional(vault.getVaultSettings().disableAllKeyrings());
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
	public void showRecoverVaultDialog() {
		recoveryKeyWindow.vault(vault).owner(window).build().showRecoveryKeyRecoverWindow();
	}

	@FXML
	public void showForgetPasswordDialog() {
		assert keychain.isSupported();
		forgetPasswordWindow.vault(vault).owner(window).build().showForgetPassword();
	}

	public BooleanExpression passwordSavedProperty() {
		return passwordSaved;
	}

	public boolean isPasswordSaved() {
		if (keychain.isSupported() && !keychain.isLocked() && vault != null) {
			return keychain.getPassphraseStoredProperty(vault.getId()).get();
		} else return false;
	}
}

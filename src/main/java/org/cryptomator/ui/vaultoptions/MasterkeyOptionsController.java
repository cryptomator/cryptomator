package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.RecoverUtil;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.forgetpassword.ForgetPasswordComponent;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.ResourceBundle;

@VaultOptionsScoped
public class MasterkeyOptionsController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MasterkeyOptionsController.class);

	private final Vault vault;
	private final Stage window;
	private final ChangePasswordComponent.Builder changePasswordWindow;
	private final RecoveryKeyComponent.Factory recoveryKeyWindow;
	private final ForgetPasswordComponent.Builder forgetPasswordWindow;
	private final KeychainManager keychain;
	private final ObservableValue<Boolean> passwordSaved;
	private final ResourceBundle resourceBundle;


	@Inject
	MasterkeyOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ChangePasswordComponent.Builder changePasswordWindow, RecoveryKeyComponent.Factory recoveryKeyWindow, ForgetPasswordComponent.Builder forgetPasswordWindow, KeychainManager keychain, //
							   ResourceBundle resourceBundle) {
		this.vault = vault;
		this.window = window;
		this.changePasswordWindow = changePasswordWindow;
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.forgetPasswordWindow = forgetPasswordWindow;
		this.keychain = keychain;
		this.resourceBundle = resourceBundle;
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
		ObjectProperty<RecoverUtil.Type> recoverTypeProperty = new SimpleObjectProperty<>(RecoverUtil.Type.SHOW_KEY);
		recoveryKeyWindow.create(vault, window, recoverTypeProperty).showRecoveryKeyCreationWindow();
	}

	@FXML
	public void showRecoverVaultDialog() {
		ObjectProperty<RecoverUtil.Type> recoverTypeProperty = new SimpleObjectProperty<>(RecoverUtil.Type.RESET_PASSWORD);
		recoveryKeyWindow.create(vault, window, recoverTypeProperty).showRecoveryKeyRecoverWindow();
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

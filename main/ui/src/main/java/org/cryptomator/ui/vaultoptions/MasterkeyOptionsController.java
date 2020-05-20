package org.cryptomator.ui.vaultoptions;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.keychain.KeychainAccessException;
import org.cryptomator.keychain.KeychainManager;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;

import javax.inject.Inject;
import java.util.Optional;

@VaultOptionsScoped
public class MasterkeyOptionsController implements FxController {

	private final Vault vault;
	private final Stage window;
	private final ChangePasswordComponent.Builder changePasswordWindow;
	private final RecoveryKeyComponent.Builder recoveryKeyWindow;
	private final Optional<KeychainManager> keychainManagerOptional;
	private final BooleanExpression passwordSaved;


	@Inject
	MasterkeyOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ChangePasswordComponent.Builder changePasswordWindow, RecoveryKeyComponent.Builder recoveryKeyWindow, Optional<KeychainManager> keychainManagerOptional) {
		this.vault = vault;
		this.window = window;
		this.changePasswordWindow = changePasswordWindow;
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.keychainManagerOptional = keychainManagerOptional;
		if (keychainManagerOptional.isPresent()) {
			this.passwordSaved = Bindings.createBooleanBinding(this::isPasswordSaved, keychainManagerOptional.get().getPassphraseStoredProperty(vault.getId()));
		} else this.passwordSaved = new SimpleBooleanProperty(false);
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

	@FXML
	public void removePasswordFromKeychain() throws KeychainAccessException {
		keychainManagerOptional.get().deletePassphrase(vault.getId());
		window.close();
	}

	public BooleanExpression passwordSavedProperty() {
		return passwordSaved;
	}

	public boolean isPasswordSaved() {
		if (keychainManagerOptional.isPresent() && vault != null) {
			return keychainManagerOptional.get().getPassphraseStoredProperty(vault.getId()).get();
		} else return false;
	}
}

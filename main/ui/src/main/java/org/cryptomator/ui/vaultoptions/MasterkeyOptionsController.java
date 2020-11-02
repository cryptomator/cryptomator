package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@VaultOptionsScoped
public class MasterkeyOptionsController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(MasterkeyOptionsController.class);

	private final Vault vault;
	private final Stage window;
	private final ChangePasswordComponent.Builder changePasswordWindow;
	private final RecoveryKeyComponent.Builder recoveryKeyWindow;
	private final KeychainManager keychain;
	private final BooleanExpression passwordSaved;


	@Inject
	MasterkeyOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ChangePasswordComponent.Builder changePasswordWindow, RecoveryKeyComponent.Builder recoveryKeyWindow, KeychainManager keychain) {
		this.vault = vault;
		this.window = window;
		this.changePasswordWindow = changePasswordWindow;
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.keychain = keychain;
		if (keychain.isSupported()) {
			this.passwordSaved = Bindings.createBooleanBinding(this::isPasswordSaved, keychain.getPassphraseStoredProperty(vault.getId()));
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
		recoveryKeyWindow.vault(vault).owner(window).build().showRecoveryKeyCreationWindow();
	}

	@FXML
	public void showRecoverVaultDialogue() {
		recoveryKeyWindow.vault(vault).owner(window).build().showRecoveryKeyRecoverWindow();
	}

	@FXML
	public void removePasswordFromKeychain() {
		assert keychain.isSupported();
		try {
			keychain.deletePassphrase(vault.getId());
		} catch (KeychainAccessException e) {
			LOG.error("Failed to delete passphrase from system keychain.", e);
		}
		window.close();
	}

	public BooleanExpression passwordSavedProperty() {
		return passwordSaved;
	}

	public boolean isPasswordSaved() {
		if (keychain.isSupported() && vault != null) {
			return keychain.getPassphraseStoredProperty(vault.getId()).get();
		} else return false;
	}
}

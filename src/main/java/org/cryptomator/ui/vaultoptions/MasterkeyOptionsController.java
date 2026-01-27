package org.cryptomator.ui.vaultoptions;

import dagger.Lazy;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.changepassword.ChangePasswordComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.forgetpassword.ForgetPasswordComponent;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.nio.file.Files;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@VaultOptionsScoped
public class MasterkeyOptionsController implements FxController {

	private static final String DOCS_URL = "https://docs.cryptomator.org/desktop/"; //TODO: replace with correct docs link

	private final Vault vault;
	private final Lazy<Application> application;
	private final Stage window;
	private final ChangePasswordComponent.Builder changePasswordWindow;
	private final RecoveryKeyComponent.Factory recoveryKeyWindow;
	private final ForgetPasswordComponent.Builder forgetPasswordWindow;
	private final KeychainManager keychain;
	private final ObservableValue<Boolean> passwordSaved;
	private final BooleanProperty masterkeyFileAvailable;


	@Inject
	MasterkeyOptionsController(@VaultOptionsWindow Vault vault, @VaultOptionsWindow Stage window, ChangePasswordComponent.Builder changePasswordWindow, RecoveryKeyComponent.Factory recoveryKeyWindow, ForgetPasswordComponent.Builder forgetPasswordWindow, KeychainManager keychain, Lazy<Application> application) {
		this.vault = vault;
		this.window = window;
		this.application = application;
		this.changePasswordWindow = changePasswordWindow;
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.forgetPasswordWindow = forgetPasswordWindow;
		this.keychain = keychain;
		if (keychain.isSupported() && !keychain.isLocked()) {
			this.passwordSaved = keychain.getPassphraseStoredProperty(vault.getId()).orElse(false);
		} else {
			this.passwordSaved = new SimpleBooleanProperty(false);
		}
		this.masterkeyFileAvailable = new SimpleBooleanProperty(Files.exists(vault.getPath().resolve(MASTERKEY_FILENAME)));
	}

	@FXML
	public void changePassword() {
		changePasswordWindow.vault(vault).owner(window).build().showChangePasswordWindow();
	}

	@FXML
	public void showRecoveryKey() {
		recoveryKeyWindow.create(vault, window, new SimpleObjectProperty<>(RecoveryActionType.SHOW_KEY)).showRecoveryKeyCreationWindow();
	}

	@FXML
	public void showRecoverVaultDialog() {
		recoveryKeyWindow.create(vault, window, new SimpleObjectProperty<>(RecoveryActionType.RESET_PASSWORD)).showRecoveryKeyRecoverWindow();
	}

	@FXML
	public void showForgetPasswordDialog() {
		assert keychain.isSupported();
		forgetPasswordWindow.vault(vault).owner(window).build().showForgetPassword();
	}

	@FXML
	public void openDocs() {
		application.get().getHostServices().showDocument(DOCS_URL);
	}

	public ObservableValue<Boolean> passwordSavedProperty() {
		return passwordSaved;
	}

	public boolean isPasswordSaved() {
		return passwordSaved.getValue();
	}

	public BooleanProperty masterkeyFileAvailableProperty() {
		return masterkeyFileAvailable;
	}

	public boolean isMasterkeyFileAvailable() {
		return masterkeyFileAvailable.get();
	}
}

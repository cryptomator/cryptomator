package org.cryptomator.ui.changepassword;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.common.BackupHelper;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import static org.cryptomator.common.Constants.MASTERKEY_BACKUP_SUFFIX;
import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@ChangePasswordScoped
public class ChangePasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final FxApplicationWindows appWindows;
	private final KeychainManager keychain;
	private final MasterkeyFileAccess masterkeyFileAccess;

	public NiceSecurePasswordField oldPasswordField;
	public CheckBox finalConfirmationCheckbox;
	public Button finishButton;
	public NewPasswordController newPasswordController;

	@Inject
	public ChangePasswordController(@ChangePasswordWindow Stage window, @ChangePasswordWindow Vault vault, FxApplicationWindows appWindows, KeychainManager keychain, MasterkeyFileAccess masterkeyFileAccess) {
		this.window = window;
		this.vault = vault;
		this.appWindows = appWindows;
		this.keychain = keychain;
		this.masterkeyFileAccess = masterkeyFileAccess;
	}

	@FXML
	public void initialize() {
		BooleanBinding checkboxNotConfirmed = finalConfirmationCheckbox.selectedProperty().not();
		BooleanBinding oldPasswordFieldEmpty = oldPasswordField.textProperty().isEmpty();
		finishButton.disableProperty().bind(checkboxNotConfirmed.or(oldPasswordFieldEmpty).or(newPasswordController.goodPasswordProperty().not()));
		window.setOnHiding(event -> {
			oldPasswordField.wipe();
			newPasswordController.passwordField.wipe();
			newPasswordController.reenterField.wipe();
		});
	}

	@FXML
	public void cancel() {
		window.close();
	}

	@FXML
	public void finish() {
		try {
			CharSequence oldPassphrase = oldPasswordField.getCharacters();
			CharSequence newPassphrase = newPasswordController.passwordField.getCharacters();
			Path masterkeyPath = vault.getPath().resolve(MASTERKEY_FILENAME);
			byte[] oldMasterkeyBytes = Files.readAllBytes(masterkeyPath);
			byte[] newMasterkeyBytes = masterkeyFileAccess.changePassphrase(oldMasterkeyBytes, oldPassphrase, newPassphrase);
			Path backupKeyPath = vault.getPath().resolve(MASTERKEY_FILENAME + BackupHelper.generateFileIdSuffix(oldMasterkeyBytes) + MASTERKEY_BACKUP_SUFFIX);
			Files.move(masterkeyPath, backupKeyPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			Files.write(masterkeyPath, newMasterkeyBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			LOG.info("Successfully changed password for {}", vault.getDisplayName());
			updatePasswordInSystemkeychain();
			window.close();
		} catch (InvalidPassphraseException e) {
			Animations.createShakeWindowAnimation(window).play();
			oldPasswordField.selectAll();
			oldPasswordField.requestFocus();
		} catch (IOException | CryptoException e) {
			LOG.error("Password change failed. Unable to perform operation.", e);
			appWindows.showErrorWindow(e, window, window.getScene());
		}
	}

	private void updatePasswordInSystemkeychain() {
		if (keychain.isSupported() && !keychain.isLocked()) {
			try {
				keychain.changePassphrase(vault.getId(), vault.getDisplayName(), newPasswordController.passwordField.getCharacters());
				LOG.info("Successfully updated password in system keychain for {}", vault.getDisplayName());
			} catch (KeychainAccessException e) {
				LOG.error("Failed to update password in system keychain.", e);
			}
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

}

package org.cryptomator.ui.changepassword;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.common.MasterkeyBackupHelper;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@ChangePasswordScoped
public class ChangePasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);
	private static final String MASTERKEY_BACKUP_SUFFIX = ".bkup";

	private final Stage window;
	private final Vault vault;
	private final ObjectProperty<CharSequence> newPassword;
	private final ErrorComponent.Builder errorComponent;
	private final KeychainManager keychain;
	private final SecureRandom csprng;
	private final MasterkeyFileAccess masterkeyFileAccess;

	public NiceSecurePasswordField oldPasswordField;
	public CheckBox finalConfirmationCheckbox;
	public Button finishButton;

	@Inject
	public ChangePasswordController(@ChangePasswordWindow Stage window, @ChangePasswordWindow Vault vault, @Named("newPassword") ObjectProperty<CharSequence> newPassword, ErrorComponent.Builder errorComponent, KeychainManager keychain, SecureRandom csprng, MasterkeyFileAccess masterkeyFileAccess) {
		this.window = window;
		this.vault = vault;
		this.newPassword = newPassword;
		this.errorComponent = errorComponent;
		this.keychain = keychain;
		this.csprng = csprng;
		this.masterkeyFileAccess = masterkeyFileAccess;
	}

	@FXML
	public void initialize() {
		BooleanBinding checkboxNotConfirmed = finalConfirmationCheckbox.selectedProperty().not();
		BooleanBinding oldPasswordFieldEmpty = oldPasswordField.textProperty().isEmpty();
		BooleanBinding newPasswordInvalid = Bindings.createBooleanBinding(() -> newPassword.get() == null || newPassword.get().length() == 0, newPassword);
		finishButton.disableProperty().bind(checkboxNotConfirmed.or(oldPasswordFieldEmpty).or(newPasswordInvalid));
	}

	@FXML
	public void cancel() {
		window.close();
	}

	@FXML
	public void finish() {
		try {
			//String normalizedOldPassphrase = Normalizer.normalize(oldPasswordField.getCharacters(), Normalizer.Form.NFC);
			//String normalizedNewPassphrase = Normalizer.normalize(newPassword.get(), Normalizer.Form.NFC);
			CharSequence oldPassphrase = oldPasswordField.getCharacters(); // TODO verify: is this already NFC-normalized?
			CharSequence newPassphrase = newPassword.get(); // TODO verify: is this already NFC-normalized?
			Path masterkeyPath = vault.getPath().resolve(MASTERKEY_FILENAME);
			byte[] oldMasterkeyBytes = Files.readAllBytes(masterkeyPath);
			byte[] newMasterkeyBytes = masterkeyFileAccess.changePassphrase(oldMasterkeyBytes, oldPassphrase, newPassphrase);
			Path backupKeyPath = vault.getPath().resolve(MASTERKEY_FILENAME + MasterkeyBackupHelper.generateFileIdSuffix(oldMasterkeyBytes) + MASTERKEY_BACKUP_SUFFIX);
			Files.move(masterkeyPath, backupKeyPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			Files.write(masterkeyPath, newMasterkeyBytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
			LOG.info("Successfully changed password for {}", vault.getDisplayName());
			window.close();
			updatePasswordInSystemkeychain();
		} catch (InvalidPassphraseException e) {
			Animations.createShakeWindowAnimation(window).play();
			oldPasswordField.selectAll();
			oldPasswordField.requestFocus();
		} catch (IOException | CryptoException e) {
			LOG.error("Password change failed. Unable to perform operation.", e);
			errorComponent.cause(e).window(window).returnToScene(window.getScene()).build().showErrorScene();
		}
	}

	private void updatePasswordInSystemkeychain() {
		if (keychain.isSupported()) {
			try {
				keychain.changePassphrase(vault.getId(), CharBuffer.wrap(newPassword.get()));
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

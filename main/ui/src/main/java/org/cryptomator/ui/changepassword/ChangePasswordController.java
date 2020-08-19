package org.cryptomator.ui.changepassword;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.keychain.KeychainAccessException;
import org.cryptomator.keychain.KeychainManager;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Optional;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@ChangePasswordScoped
public class ChangePasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final ObjectProperty<CharSequence> newPassword;
	private final ErrorComponent.Builder errorComponent;
	private final Optional<KeychainManager> keychain;

	public NiceSecurePasswordField oldPasswordField;
	public CheckBox finalConfirmationCheckbox;
	public Button finishButton;

	@Inject
	public ChangePasswordController(@ChangePasswordWindow Stage window, @ChangePasswordWindow Vault vault, @Named("newPassword") ObjectProperty<CharSequence> newPassword, ErrorComponent.Builder errorComponent, Optional<KeychainManager> keychain) {
		this.window = window;
		this.vault = vault;
		this.newPassword = newPassword;
		this.errorComponent = errorComponent;
		this.keychain = keychain;
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
			CryptoFileSystemProvider.changePassphrase(vault.getPath(), MASTERKEY_FILENAME, oldPasswordField.getCharacters(), newPassword.get());
			LOG.info("Successfully changed password for {}", vault.getDisplayableName());
			window.close();
			updatePasswordInSystemkeychain();
		} catch (IOException e) {
			LOG.error("IO error occured during password change. Unable to perform operation.", e);
			errorComponent.cause(e).window(window).returnToScene(window.getScene()).build().showErrorScene();
		} catch (InvalidPassphraseException e) {
			Animations.createShakeWindowAnimation(window).play();
			oldPasswordField.selectAll();
			oldPasswordField.requestFocus();
		}
	}

	private void updatePasswordInSystemkeychain() {
		if (keychain.isPresent()) {
			try {
				keychain.get().changePassphrase(vault.getId(), CharBuffer.wrap(newPassword.get()));
				LOG.info("Successfully updated password in system keychain for {}", vault.getDisplayableName());
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

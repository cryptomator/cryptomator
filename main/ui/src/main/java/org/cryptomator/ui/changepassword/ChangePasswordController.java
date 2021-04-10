package org.cryptomator.ui.changepassword;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.integrations.keychain.KeychainAccessException;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.error.GenericErrorComponent;
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

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

@ChangePasswordScoped
public class ChangePasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final ObjectProperty<CharSequence> newPassword;
	private final GenericErrorComponent.Builder genericErrorBuilder;
	private final KeychainManager keychain;

	public NiceSecurePasswordField oldPasswordField;
	public CheckBox finalConfirmationCheckbox;
	public Button finishButton;

	@Inject
	public ChangePasswordController(@ChangePasswordWindow Stage window, @ChangePasswordWindow Vault vault, @Named("newPassword") ObjectProperty<CharSequence> newPassword, GenericErrorComponent.Builder genericErrorBuilder, KeychainManager keychain) {
		this.window = window;
		this.vault = vault;
		this.newPassword = newPassword;
		this.genericErrorBuilder = genericErrorBuilder;
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
			LOG.info("Successfully changed password for {}", vault.getDisplayName());
			window.close();
			updatePasswordInSystemkeychain();
		} catch (IOException e) {
			LOG.error("IO error occured during password change. Unable to perform operation.", e);
			genericErrorBuilder.cause(e).window(window).returnToScene(window.getScene()).build().showErrorScene();
		} catch (InvalidPassphraseException e) {
			Animations.createShakeWindowAnimation(window).play();
			oldPasswordField.selectAll();
			oldPasswordField.requestFocus();
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

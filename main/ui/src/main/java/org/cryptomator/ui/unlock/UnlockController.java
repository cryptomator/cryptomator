package org.cryptomator.ui.unlock;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.cryptomator.ui.util.DialogBuilderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

@UnlockScoped
public class UnlockController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);

	private final Stage window;
	private final Vault vault;
	private final ExecutorService executor;
	private final ObjectBinding<ContentDisplay> unlockButtonState;
	private final Optional<KeychainAccess> keychainAccess;
	private final ResourceBundle resourceBundle;
	public SecPasswordField passwordField;
	public CheckBox savePassword;

	@Inject
	public UnlockController(@UnlockWindow Stage window, @UnlockWindow Vault vault, ExecutorService executor, Optional<KeychainAccess> keychainAccess, ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.executor = executor;
		this.unlockButtonState = Bindings.createObjectBinding(this::getUnlockButtonState, vault.stateProperty());
		this.keychainAccess = keychainAccess;
		this.resourceBundle = resourceBundle;
	}

	public void initialize() {
		if (keychainAccess.isPresent()) {
			loadStoredPassword();
		} else {
			savePassword.setDisable(true);
		}
	}

	@FXML
	public void cancel() {
		LOG.debug("Unlock canceled by user.");
		window.close();
	}

	@FXML
	public void unlock() {
		CharSequence password = passwordField.getCharacters();
		Tasks.create(() -> {
			vault.unlock(password);
			if (keychainAccess.isPresent() && savePassword.isSelected()) {
				keychainAccess.get().storePassphrase(vault.getId(), password);
			}
		}).onSuccess(() -> {
			passwordField.swipe();
			LOG.info("Unlock of '{}' succeeded.", vault.getDisplayableName());
			window.close();
		}).onError(InvalidPassphraseException.class, e -> {
			passwordField.selectAll();
			passwordField.requestFocus();
		}).onError(UnsupportedVaultFormatException.class, e -> {
			// TODO
		}).onError(NotDirectoryException.class, e -> {
			LOG.error("Unlock failed. Mount point not a directory: {}", e.getMessage());
			// TODO
		}).onError(DirectoryNotEmptyException.class, e -> {
			LOG.error("Unlock failed. Mount point not empty: {}", e.getMessage());
			// TODO
		}).onError(Exception.class, e -> { // including RuntimeExceptions
			LOG.error("Unlock failed for technical reasons.", e);
			// TODO
		}).runOnce(executor);
	}

	/* Save Password */

	@FXML
	private void didClickSavePasswordCheckbox() {
		if (!savePassword.isSelected() && hasStoredPassword()) {
			Alert confirmDialog = DialogBuilderUtil.buildConfirmationDialog( //
					resourceBundle.getString("unlock.deleteSavedPasswordDialog.title"), //
					resourceBundle.getString("unlock.deleteSavedPasswordDialog.header"), //
					resourceBundle.getString("unlock.deleteSavedPasswordDialog.content"), //
					SystemUtils.IS_OS_MAC_OSX ? ButtonType.CANCEL : ButtonType.OK);
			Optional<ButtonType> choice = confirmDialog.showAndWait();
			if (ButtonType.OK.equals(choice.get())) {
				keychainAccess.get().deletePassphrase(vault.getId());
			} else if (ButtonType.CANCEL.equals(choice.get())) {
				savePassword.setSelected(true);
			}
		}
	}

	private void loadStoredPassword() {
		assert keychainAccess.isPresent();
		char[] storedPw = keychainAccess.get().loadPassphrase(vault.getId());
		if (storedPw != null) {
			savePassword.setSelected(true);
			passwordField.setPassword(storedPw);
			passwordField.selectRange(storedPw.length, storedPw.length);
			Arrays.fill(storedPw, ' ');
		}
	}

	private boolean hasStoredPassword() {
		assert keychainAccess.isPresent();
		char[] storedPw = keychainAccess.get().loadPassphrase(vault.getId());
		if (storedPw != null) {
			Arrays.fill(storedPw, ' ');
			return true;
		} else {
			return false;
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public ObjectBinding<ContentDisplay> unlockButtonStateProperty() {
		return unlockButtonState;
	}

	public ContentDisplay getUnlockButtonState() {
		switch (vault.getState()) {
			case PROCESSING:
				return ContentDisplay.LEFT;
			default:
				return ContentDisplay.TEXT_ONLY;
		}
	}
}

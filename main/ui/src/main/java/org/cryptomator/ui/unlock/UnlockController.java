package org.cryptomator.ui.unlock;

import dagger.Lazy;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.keychain.KeychainAccessException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.controls.SecPasswordField;
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
	private final Lazy<Scene> successScene;
	private final BooleanProperty unlockButtonDisabled;
	public SecPasswordField passwordField;
	public CheckBox savePassword;

	@Inject
	public UnlockController(@UnlockWindow Stage window, @UnlockWindow Vault vault, ExecutorService executor, Optional<KeychainAccess> keychainAccess, ResourceBundle resourceBundle, @FxmlScene(FxmlFile.UNLOCK_SUCCESS) Lazy<Scene> successScene) {
		this.window = window;
		this.vault = vault;
		this.executor = executor;
		this.unlockButtonState = Bindings.createObjectBinding(this::getUnlockButtonState, vault.stateProperty());
		this.keychainAccess = keychainAccess;
		this.resourceBundle = resourceBundle;
		this.successScene = successScene;
		this.unlockButtonDisabled = new SimpleBooleanProperty();
	}

	public void initialize() {
		if (keychainAccess.isPresent()) {
			loadStoredPassword();
		} else {
			savePassword.setDisable(true);
		}
		unlockButtonDisabled.bind(vault.stateProperty().isNotEqualTo(Vault.State.LOCKED).or(passwordField.textProperty().isEmpty()));
	}

	@FXML
	public void cancel() {
		LOG.debug("Unlock canceled by user.");
		window.close();
	}

	@FXML
	public void unlock() {
		LOG.trace("UnlockController.unlock()");
		CharSequence password = passwordField.getCharacters();
		vault.setState(Vault.State.PROCESSING);
		Tasks.create(() -> {
			vault.unlock(password);
			if (keychainAccess.isPresent() && savePassword.isSelected()) {
				keychainAccess.get().storePassphrase(vault.getId(), password);
			}
		}).onSuccess(() -> {
			vault.setState(Vault.State.UNLOCKED);
			passwordField.swipe();
			LOG.info("Unlock of '{}' succeeded.", vault.getDisplayableName());
			window.setScene(successScene.get());
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
		}).andFinally(() -> {
			if (!vault.isUnlocked()) {
				vault.setState(Vault.State.LOCKED);
			}
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
				try {
					keychainAccess.get().deletePassphrase(vault.getId());
				} catch (KeychainAccessException e) {
					LOG.error("Failed to remove entry from system keychain.", e);
				}
			} else if (ButtonType.CANCEL.equals(choice.get())) {
				savePassword.setSelected(true);
			}
		}
	}

	private void loadStoredPassword() {
		assert keychainAccess.isPresent();
		char[] storedPw = new char[0];
		try {
			storedPw = keychainAccess.get().loadPassphrase(vault.getId());
			if (storedPw != null) {
				savePassword.setSelected(true);
				passwordField.setPassword(storedPw);
				passwordField.selectRange(storedPw.length, storedPw.length);
			}
		} catch (KeychainAccessException e) {
			LOG.error("Failed to load entry from system keychain.", e);
		} finally {
			Arrays.fill(storedPw, ' ');
		}
	}

	private boolean hasStoredPassword() {
		char[] storedPw = new char[0];
		try {
			storedPw = keychainAccess.get().loadPassphrase(vault.getId());
			return storedPw.length != 0;
		} catch (KeychainAccessException e) {
			return false;
		} finally {
			Arrays.fill(storedPw, ' ');
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

	public ReadOnlyBooleanProperty unlockButtonDisabledProperty() {
		return unlockButtonDisabled;
	}

	public boolean isUnlockButtonDisabled() {
		return unlockButtonDisabled.get();
	}
}

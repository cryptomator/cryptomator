package org.cryptomator.ui.unlock;

import dagger.Lazy;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.keychain.KeychainAccessException;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@UnlockScoped
public class UnlockController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);

	private final Stage window;
	private final Vault vault;
	private final ExecutorService executor;
	private final ObjectBinding<ContentDisplay> unlockButtonState;
	private final Optional<KeychainAccess> keychainAccess;
	private final VaultService vaultService;
	private final Lazy<Scene> successScene;
	private final Lazy<Scene> invalidMountPointScene;
	private final ErrorComponent.Builder errorComponent;
	private final ForgetPasswordComponent.Builder forgetPassword;
	private final BooleanProperty unlockButtonDisabled;
	public NiceSecurePasswordField passwordField;
	public CheckBox savePassword;

	@Inject
	public UnlockController(@UnlockWindow Stage window, @UnlockWindow Vault vault, ExecutorService executor, Optional<KeychainAccess> keychainAccess, VaultService vaultService, @FxmlScene(FxmlFile.UNLOCK_SUCCESS) Lazy<Scene> successScene, @FxmlScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT) Lazy<Scene> invalidMountPointScene, ErrorComponent.Builder errorComponent, ForgetPasswordComponent.Builder forgetPassword) {
		this.window = window;
		this.vault = vault;
		this.executor = executor;
		this.unlockButtonState = Bindings.createObjectBinding(this::getUnlockButtonState, vault.stateProperty());
		this.keychainAccess = keychainAccess;
		this.vaultService = vaultService;
		this.successScene = successScene;
		this.invalidMountPointScene = invalidMountPointScene;
		this.errorComponent = errorComponent;
		this.forgetPassword = forgetPassword;
		this.unlockButtonDisabled = new SimpleBooleanProperty();
	}

	public void initialize() {
		if (keychainAccess.isPresent()) {
			loadStoredPassword();
		} else {
			savePassword.setDisable(true);
		}
		unlockButtonDisabled.bind(vault.stateProperty().isNotEqualTo(VaultState.LOCKED).or(passwordField.textProperty().isEmpty()));
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

		Task<Vault> task = vaultService.createUnlockTask(vault, password);
		passwordField.setDisable(true);
		task.setOnSucceeded(event -> {
			passwordField.setDisable(false);
			if (keychainAccess.isPresent() && savePassword.isSelected()) {
				try {
					keychainAccess.get().storePassphrase(vault.getId(), password);
				} catch (KeychainAccessException e) {
					LOG.error("Failed to store passphrase in system keychain.", e);
				}
			}
			passwordField.swipe();
			LOG.info("Unlock of '{}' succeeded.", vault.getDisplayableName());
			window.setScene(successScene.get());
		});
		task.setOnFailed(event -> {
			passwordField.setDisable(false);
			if (task.getException() instanceof InvalidPassphraseException) {
				Animations.createShakeWindowAnimation(window).play();
				passwordField.selectAll();
				passwordField.requestFocus();
			} else if (task.getException() instanceof NotDirectoryException || task.getException() instanceof DirectoryNotEmptyException) {
				LOG.error("Unlock failed. Mount point not an empty directory: {}", task.getException().getMessage());
				window.setScene(invalidMountPointScene.get());
			} else {
				LOG.error("Unlock failed for technical reasons.", task.getException());
				errorComponent.cause(task.getException()).window(window).returnToScene(window.getScene()).build().showErrorScene();
			}
		});
		executor.execute(task);
	}

	/* Save Password */

	@FXML
	private void didClickSavePasswordCheckbox() {
		if (!savePassword.isSelected() && hasStoredPassword()) {
			forgetPassword.vault(vault).owner(window).build().showForgetPassword().thenAccept(forgotten -> savePassword.setSelected(!forgotten));
		}
	}

	private void loadStoredPassword() {
		assert keychainAccess.isPresent();
		char[] storedPw = null;
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
			if (storedPw != null) {
				Arrays.fill(storedPw, ' ');
			}
		}
	}

	private boolean hasStoredPassword() {
		char[] storedPw = null;
		try {
			storedPw = keychainAccess.get().loadPassphrase(vault.getId());
			return storedPw != null;
		} catch (KeychainAccessException e) {
			return false;
		} finally {
			if (storedPw != null) {
				Arrays.fill(storedPw, ' ');
			}
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
		return switch (vault.getState()) {
			case PROCESSING -> ContentDisplay.LEFT;
			default -> ContentDisplay.TEXT_ONLY;
		};
	}

	public ReadOnlyBooleanProperty unlockButtonDisabledProperty() {
		return unlockButtonDisabled;
	}

	public boolean isUnlockButtonDisabled() {
		return unlockButtonDisabled.get();
	}
}

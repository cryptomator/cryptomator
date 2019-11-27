package org.cryptomator.ui.unlock;

import dagger.Lazy;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.WritableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.keychain.KeychainAccessException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
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
	private final Lazy<Scene> successScene;
	private final Lazy<Scene> genericErrorScene;
	private final ObjectProperty<Exception> genericErrorCause;
	private final ForgetPasswordComponent.Builder forgetPassword;
	private final BooleanProperty unlockButtonDisabled;
	public NiceSecurePasswordField passwordField;
	public CheckBox savePassword;

	@Inject
	public UnlockController(@UnlockWindow Stage window, @UnlockWindow Vault vault, ExecutorService executor, Optional<KeychainAccess> keychainAccess, @FxmlScene(FxmlFile.UNLOCK_SUCCESS) Lazy<Scene> successScene, @FxmlScene(FxmlFile.UNLOCK_GENERIC_ERROR) Lazy<Scene> genericErrorScene, @Named("genericErrorCause") ObjectProperty<Exception> genericErrorCause, ForgetPasswordComponent.Builder forgetPassword) {
		this.window = window;
		this.vault = vault;
		this.executor = executor;
		this.unlockButtonState = Bindings.createObjectBinding(this::getUnlockButtonState, vault.stateProperty());
		this.keychainAccess = keychainAccess;
		this.successScene = successScene;
		this.genericErrorScene = genericErrorScene;
		this.genericErrorCause = genericErrorCause;
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
		vault.setState(VaultState.PROCESSING);
		Tasks.create(() -> {
			vault.unlock(password);
			if (keychainAccess.isPresent() && savePassword.isSelected()) {
				keychainAccess.get().storePassphrase(vault.getId(), password);
			}
		}).onSuccess(() -> {
			vault.setState(VaultState.UNLOCKED);
			passwordField.swipe();
			LOG.info("Unlock of '{}' succeeded.", vault.getDisplayableName());
			window.setScene(successScene.get());
		}).onError(InvalidPassphraseException.class, e -> {
			shakeWindow();
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
			genericErrorCause.set(e);
			window.setScene(genericErrorScene.get());
		}).andFinally(() -> {
			if (!vault.isUnlocked()) {
				vault.setState(VaultState.LOCKED);
			}
		}).runOnce(executor);
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

	/* Animations */

	private void shakeWindow() {
		WritableValue<Double> writableWindowX = new WritableValue<>() {
			@Override
			public Double getValue() {
				return window.getX();
			}

			@Override
			public void setValue(Double value) {
				window.setX(value);
			}
		};
		Timeline timeline = new Timeline( //
				new KeyFrame(Duration.ZERO, new KeyValue(writableWindowX, window.getX())), //
				new KeyFrame(new Duration(100), new KeyValue(writableWindowX, window.getX() - 22.0)), //
				new KeyFrame(new Duration(200), new KeyValue(writableWindowX, window.getX() + 18.0)), //
				new KeyFrame(new Duration(300), new KeyValue(writableWindowX, window.getX() - 14.0)), //
				new KeyFrame(new Duration(400), new KeyValue(writableWindowX, window.getX() + 10.0)), //
				new KeyFrame(new Duration(500), new KeyValue(writableWindowX, window.getX() - 6.0)), //
				new KeyFrame(new Duration(600), new KeyValue(writableWindowX, window.getX() + 2.0)), //
				new KeyFrame(new Duration(700), new KeyValue(writableWindowX, window.getX())) //
		);
		timeline.play();
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

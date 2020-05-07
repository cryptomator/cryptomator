package org.cryptomator.ui.unlock;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.keychain.KeychainAccess;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.forgetPassword.ForgetPasswordComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@UnlockScoped
public class UnlockController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);

	private final Stage window;
	private final Vault vault;
	private final AtomicReference<char[]> password;
	private final AtomicBoolean savePassword;
	private final Optional<char[]> savedPassword;
	private final UserInteractionLock<UnlockModule.PasswordEntry> passwordEntryLock;
	private final ForgetPasswordComponent.Builder forgetPassword;
	private final Optional<KeychainAccess> keychainAccess;
	private final ObjectBinding<ContentDisplay> unlockButtonContentDisplay;
	private final BooleanBinding userInteractionDisabled;
	private final BooleanProperty unlockButtonDisabled;
	public NiceSecurePasswordField passwordField;
	public CheckBox savePasswordCheckbox;

	@Inject
	public UnlockController(@UnlockWindow Stage window, @UnlockWindow Vault vault, AtomicReference<char[]> password, @Named("savePassword") AtomicBoolean savePassword, @Named("savedPassword") Optional<char[]> savedPassword, UserInteractionLock<UnlockModule.PasswordEntry> passwordEntryLock, ForgetPasswordComponent.Builder forgetPassword, Optional<KeychainAccess> keychainAccess) {
		this.window = window;
		this.vault = vault;
		this.password = password;
		this.savePassword = savePassword;
		this.savedPassword = savedPassword;
		this.passwordEntryLock = passwordEntryLock;
		this.forgetPassword = forgetPassword;
		this.keychainAccess = keychainAccess;
		this.unlockButtonContentDisplay = Bindings.createObjectBinding(this::getUnlockButtonContentDisplay, passwordEntryLock.awaitingInteraction());
		this.userInteractionDisabled = passwordEntryLock.awaitingInteraction().not();
		this.unlockButtonDisabled = new SimpleBooleanProperty();
	}

	public void initialize() {
		savePasswordCheckbox.setSelected(savedPassword.isPresent());
		if (password.get() != null) {
			passwordField.setPassword(password.get());
		}
		unlockButtonDisabled.bind(userInteractionDisabled.or(passwordField.textProperty().isEmpty()));
	}

	@FXML
	public void cancel() {
		LOG.debug("Unlock canceled by user.");
		window.close();
		passwordEntryLock.interacted(UnlockModule.PasswordEntry.CANCELED);
	}

	@FXML
	public void unlock() {
		LOG.trace("UnlockController.unlock()");
		CharSequence pwFieldContents = passwordField.getCharacters();
		char[] pw = new char[pwFieldContents.length()];
		for (int i = 0; i < pwFieldContents.length(); i++) {
			pw[i] = pwFieldContents.charAt(i);
		}
		password.set(pw);
		passwordEntryLock.interacted(UnlockModule.PasswordEntry.PASSWORD_ENTERED);
	}

	/* Save Password */

	@FXML
	private void didClickSavePasswordCheckbox() {
		savePassword.set(savePasswordCheckbox.isSelected());
		if (!savePasswordCheckbox.isSelected() && savedPassword.isPresent()) {
			forgetPassword.vault(vault).owner(window).build().showForgetPassword().thenAccept(forgotten -> savePasswordCheckbox.setSelected(!forgotten));
		}
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public ObjectBinding<ContentDisplay> unlockButtonContentDisplayProperty() {
		return unlockButtonContentDisplay;
	}

	public ContentDisplay getUnlockButtonContentDisplay() {
		return passwordEntryLock.awaitingInteraction().get() ? ContentDisplay.TEXT_ONLY : ContentDisplay.LEFT;
	}

	public BooleanBinding userInteractionDisabledProperty() {
		return userInteractionDisabled;
	}

	public boolean isUserInteractionDisabled() {
		return userInteractionDisabled.get();
	}

	public ReadOnlyBooleanProperty unlockButtonDisabledProperty() {
		return unlockButtonDisabled;
	}

	public boolean isUnlockButtonDisabled() {
		return unlockButtonDisabled.get();
	}

	public boolean isKeychainAccessAvailable() {
		return keychainAccess.isPresent();
	}
}

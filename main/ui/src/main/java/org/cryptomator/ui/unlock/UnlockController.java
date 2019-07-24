package org.cryptomator.ui.unlock;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ContentDisplay;
import javafx.stage.Stage;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.UnsupportedVaultFormatException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.controls.SecPasswordField;
import org.cryptomator.ui.model.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.NotDirectoryException;
import java.util.concurrent.ExecutorService;

@UnlockScoped
public class UnlockController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UnlockController.class);

	private final Stage window;
	private final ReadOnlyObjectProperty<Vault> vault;
	private final ExecutorService executor;
	private final ObjectBinding<ContentDisplay> unlockButtonState;
	public SecPasswordField passwordField;

	@Inject
	public UnlockController(@UnlockWindow Stage window, @UnlockWindow ReadOnlyObjectProperty<Vault> vault, ExecutorService executor) {
		this.window = window;
		this.vault = vault;
		this.executor = executor;
		this.unlockButtonState = Bindings.createObjectBinding(this::getUnlockButtonState, vault.get().stateProperty());
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
			vault.get().unlock(password);
//			if (keychainAccess.isPresent() && savePassword.isSelected()) {
//				keychainAccess.get().storePassphrase(vault.getId(), password);
//			}
		}).onSuccess(() -> {
			passwordField.swipe();
			LOG.info("Unlock of '{}' succeeded.", vault.get().getDisplayableName());
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

	/* Getter/Setter */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

	public ObjectBinding<ContentDisplay> unlockButtonStateProperty() {
		return unlockButtonState;
	}

	public ContentDisplay getUnlockButtonState() {
		switch (vault.get().getState()) {
			case PROCESSING:
				return ContentDisplay.LEFT;
			default:
				return ContentDisplay.TEXT_ONLY;
		}
	}
}

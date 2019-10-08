package org.cryptomator.ui.recoverykey;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

@RecoveryKeyScoped
public class RecoveryKeyCreationController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyCreationController.class);

	private final Stage window;
	private final Vault vault;
	private final ExecutorService executor;
	private final CharSequence prefilledPassword;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final StringProperty recoveryKey;
	public NiceSecurePasswordField passwordField;

	@Inject
	public RecoveryKeyCreationController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, RecoveryKeyFactory recoveryKeyFactory, ExecutorService executor, @Nullable CharSequence prefilledPassword) {
		this.window = window;
		this.vault = vault;
		this.executor = executor;
		this.prefilledPassword = prefilledPassword;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.recoveryKey = new SimpleStringProperty();
	}
	
	@FXML
	public void initialize() {
		if (prefilledPassword != null) {
			passwordField.setPassword(prefilledPassword);
		}
	}
	
	@FXML
	public void createRecoveryKey() {
		Tasks.create(() -> {
			return recoveryKeyFactory.createRecoveryKey(vault.getPath(), passwordField.getCharacters());
		}).onSuccess(result -> {
			recoveryKey.set(result);
		}).onError(IOException.class, e -> {
			LOG.error("Creation of recovery key failed.", e);
		}).onError(InvalidPassphraseException.class, e -> {
			// TODO shake animation? :D
		}).runOnce(executor);
	}

	@FXML
	public void close() {
		window.close();
	}
	
	/* Getter/Setter */

	public ReadOnlyStringProperty recoveryKeyProperty() {
		return recoveryKey;
	}

	public String getRecoveryKey() {
		return recoveryKey.get();
	}
}

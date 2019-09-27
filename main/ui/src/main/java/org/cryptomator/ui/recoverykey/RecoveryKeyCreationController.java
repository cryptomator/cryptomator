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
	private static final String MASTERKEY_FILENAME = "masterkey.cryptomator"; // TODO: deduplicate constant declared in multiple classes

	private final Stage window;
	private final Vault vault;
	private final ExecutorService executor;
	private final CharSequence prefilledPassword;
	private final WordEncoder wordEncoder;
	private final StringProperty recoveryKey;
	public NiceSecurePasswordField passwordField;

	@Inject
	public RecoveryKeyCreationController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, ExecutorService executor, @Nullable CharSequence prefilledPassword) {
		this.window = window;
		this.vault = vault;
		this.executor = executor;
		this.prefilledPassword = prefilledPassword;
		this.wordEncoder = new WordEncoder();
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
			byte[] rawKey = CryptoFileSystemProvider.exportRawKey(vault.getPath(), MASTERKEY_FILENAME, new byte[0], passwordField.getCharacters());
			assert rawKey.length == 64;
			byte[] paddedKey = Arrays.copyOf(rawKey, 66);
			// TODO add two-byte CRC

			try {
				return wordEncoder.encodePadded(paddedKey);
			} finally {
				Arrays.fill(rawKey, (byte) 0x00);
				Arrays.fill(paddedKey, (byte) 0x00);
			}
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

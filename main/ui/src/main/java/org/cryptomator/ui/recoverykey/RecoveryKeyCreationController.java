package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

@RecoveryKeyScoped
public class RecoveryKeyCreationController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyCreationController.class);

	private final Stage window;
	private final Lazy<Scene> successScene;
	private final Vault vault;
	private final ExecutorService executor;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final StringProperty recoveryKeyProperty;
	public NiceSecurePasswordField passwordField;

	@Inject
	public RecoveryKeyCreationController(@RecoveryKeyWindow Stage window, @FxmlScene(FxmlFile.RECOVERYKEY_SUCCESS) Lazy<Scene> successScene, @RecoveryKeyWindow Vault vault, RecoveryKeyFactory recoveryKeyFactory, ExecutorService executor, @RecoveryKeyWindow StringProperty recoveryKey) {
		this.window = window;
		this.successScene = successScene;
		this.vault = vault;
		this.executor = executor;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.recoveryKeyProperty = recoveryKey;
	}
	
	@FXML
	public void createRecoveryKey() {
		Tasks.create(() -> {
			return recoveryKeyFactory.createRecoveryKey(vault.getPath(), passwordField.getCharacters());
		}).onSuccess(result -> {
			recoveryKeyProperty.set(result);
			window.setScene(successScene.get());
		}).onError(IOException.class, e -> {
			LOG.error("Creation of recovery key failed.", e);
		}).onError(InvalidPassphraseException.class, e -> {
			Animations.createShakeWindowAnimation(window).play();
		}).runOnce(executor);
	}

	@FXML
	public void close() {
		window.close();
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}
}

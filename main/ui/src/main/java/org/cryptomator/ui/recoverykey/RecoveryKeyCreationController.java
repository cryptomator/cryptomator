package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
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
	private final ErrorComponent.Builder errorComponent;
	public NiceSecurePasswordField passwordField;

	@Inject
	public RecoveryKeyCreationController(@RecoveryKeyWindow Stage window, @FxmlScene(FxmlFile.RECOVERYKEY_SUCCESS) Lazy<Scene> successScene, @RecoveryKeyWindow Vault vault, RecoveryKeyFactory recoveryKeyFactory, ExecutorService executor, @RecoveryKeyWindow StringProperty recoveryKey, ErrorComponent.Builder errorComponent) {
		this.window = window;
		this.successScene = successScene;
		this.vault = vault;
		this.executor = executor;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.recoveryKeyProperty = recoveryKey;
		this.errorComponent = errorComponent;
	}

	@FXML
	public void createRecoveryKey() {
		Task<String> task = new RecoveryKeyCreationTask();
		task.setOnScheduled(event -> {
			LOG.debug("Creating recovery key for {}.", vault.getDisplayablePath());
		});
		task.setOnSucceeded(event -> {
			String recoveryKey = task.getValue();
			recoveryKeyProperty.set(recoveryKey);
			window.setScene(successScene.get());
		});
		task.setOnFailed(event -> {
			if (task.getException() instanceof InvalidPassphraseException) {
				Animations.createShakeWindowAnimation(window).play();
			} else {
				LOG.error("Creation of recovery key failed.", task.getException());
				errorComponent.cause(task.getException()).window(window).returnToScene(window.getScene()).build().showErrorScene();
			}
		});
		executor.submit(task);
	}

	@FXML
	public void close() {
		window.close();
	}

	private class RecoveryKeyCreationTask extends Task<String> {

		private RecoveryKeyCreationTask() {
			setOnFailed(event -> LOG.error("Failed to create recovery key", getException()));
		}

		@Override
		protected String call() throws IOException {
			return recoveryKeyFactory.createRecoveryKey(vault.getPath(), passwordField.getCharacters());
		}

	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}
}

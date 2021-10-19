package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.NewPasswordController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

@RecoveryKeyScoped
public class RecoveryKeyResetPasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyResetPasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final ExecutorService executor;
	private final StringProperty recoveryKey;
	private final Lazy<Scene> recoverScene;
	private final ErrorComponent.Builder errorComponent;

	public NewPasswordController newPasswordController;

	@Inject
	public RecoveryKeyResetPasswordController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, RecoveryKeyFactory recoveryKeyFactory, ExecutorService executor, @RecoveryKeyWindow StringProperty recoveryKey, @FxmlScene(FxmlFile.RECOVERYKEY_RECOVER) Lazy<Scene> recoverScene, ErrorComponent.Builder errorComponent) {
		this.window = window;
		this.vault = vault;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.executor = executor;
		this.recoveryKey = recoveryKey;
		this.recoverScene = recoverScene;
		this.errorComponent = errorComponent;
	}

	@FXML
	public void back() {
		window.setScene(recoverScene.get());
	}

	@FXML
	public void done() {
		Task<Void> task = new ResetPasswordTask();
		task.setOnScheduled(event -> {
			LOG.debug("Using recovery key to reset password for {}.", vault.getDisplayablePath());
		});
		task.setOnSucceeded(event -> {
			LOG.info("Used recovery key to reset password for {}.", vault.getDisplayablePath());
			// TODO show success screen
			window.close();
		});
		task.setOnFailed(event -> {
			LOG.error("Resetting password failed.", task.getException());
			errorComponent.cause(task.getException()).window(window).returnToScene(recoverScene.get()).build().showErrorScene();
		});
		executor.submit(task);
	}

	private class ResetPasswordTask extends Task<Void> {

		private ResetPasswordTask() {
			setOnFailed(event -> LOG.error("Failed to reset password", getException()));
		}

		@Override
		protected Void call() throws IOException, IllegalArgumentException {
			recoveryKeyFactory.resetPasswordWithRecoveryKey(vault.getPath(), recoveryKey.get(), newPasswordController.passwordField.getCharacters());
			return null;
		}

	}

	/* Getter/Setter */

	public ReadOnlyBooleanProperty validPasswordProperty() {
		return newPasswordController.goodPasswordProperty();
	}

	public boolean isValidPassword() {
		return newPasswordController.isGoodPassword();
	}

}

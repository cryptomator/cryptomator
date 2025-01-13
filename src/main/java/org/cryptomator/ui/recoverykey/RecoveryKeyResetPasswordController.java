package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptofs.CryptoFileSystemProperties;
import org.cryptomator.cryptofs.CryptoFileSystemProvider;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.api.MasterkeyLoader;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.changepassword.NewPasswordController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;

import static org.cryptomator.common.Constants.DEFAULT_KEY_ID;
import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;
import static org.cryptomator.common.Constants.VAULTCONFIG_FILENAME;

@RecoveryKeyScoped
public class RecoveryKeyResetPasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyResetPasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final ExecutorService executor;
	private final StringProperty recoveryKey;
	private final Lazy<Scene> recoverResetPasswordSuccessScene;
	private final Lazy<Scene> recoverResetVaultConfigSuccessScene;
	private final FxApplicationWindows appWindows;
	private final MasterkeyFileAccess masterkeyFileAccess;

	public NewPasswordController newPasswordController;

	@Inject
	public RecoveryKeyResetPasswordController(@RecoveryKeyWindow Stage window, //
											  @RecoveryKeyWindow Vault vault, //
											  RecoveryKeyFactory recoveryKeyFactory, //
											  ExecutorService executor, //
											  @RecoveryKeyWindow StringProperty recoveryKey, //
											  @FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD_SUCCESS) Lazy<Scene> recoverResetPasswordSuccessScene, //
											  @FxmlScene(FxmlFile.RECOVERYKEY_RESET_VAULT_CONFIG_SUCCESS) Lazy<Scene> recoverResetVaultConfigSuccessScene, //
											  FxApplicationWindows appWindows, //
											  MasterkeyFileAccess masterkeyFileAccess) {
		this.window = window;
		this.vault = vault;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.executor = executor;
		this.recoveryKey = recoveryKey;
		this.recoverResetPasswordSuccessScene = recoverResetPasswordSuccessScene;
		this.recoverResetVaultConfigSuccessScene = recoverResetVaultConfigSuccessScene;
		this.appWindows = appWindows;
		this.masterkeyFileAccess = masterkeyFileAccess;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void resetPassword() {
		if(vault.isMissingVaultConfig()){
			Path vaultPath = vault.getPath();
			Path recoveryPath = vaultPath.resolve("r");
			try {
				Files.createDirectory(recoveryPath);
				recoveryKeyFactory.newMasterkeyFileWithPassphrase(recoveryPath, recoveryKey.get(), newPasswordController.passwordField.getCharacters());
			} catch (IOException e) {
				LOG.error("Creating directory or recovering masterkey failed", e);
			}

			Path masterkeyFilePath = recoveryPath.resolve(MASTERKEY_FILENAME);
			try (Masterkey masterkey = masterkeyFileAccess.load(masterkeyFilePath, newPasswordController.passwordField.getCharacters())) {
				try {
					MasterkeyLoader loader = ignored -> masterkey.copy();
					CryptoFileSystemProperties fsProps = CryptoFileSystemProperties.cryptoFileSystemProperties() //
							.withCipherCombo(CryptorProvider.Scheme.SIV_CTRMAC) //
							.withKeyLoader(loader) //
							.withShorteningThreshold(220) //
							.build();
					CryptoFileSystemProvider.initialize(recoveryPath, fsProps, DEFAULT_KEY_ID);
				} catch (CryptoException | IOException e) {
					LOG.error("Recovering vault failed", e);
				}
				Files.move(masterkeyFilePath, vaultPath.resolve(MASTERKEY_FILENAME), StandardCopyOption.REPLACE_EXISTING);
				Files.move(recoveryPath.resolve(VAULTCONFIG_FILENAME), vaultPath.resolve(VAULTCONFIG_FILENAME));
				try (var paths = Files.walk(recoveryPath)) {
					paths.sorted(Comparator.reverseOrder()).forEach(p -> {
						try {
							Files.delete(p);
						} catch (IOException e) {
							LOG.info("Unable to delete {}. Please delete it manually.", p);
						}
					});
				}
				window.setScene(recoverResetVaultConfigSuccessScene.get());
			} catch (IOException e) {
				LOG.error("Moving recovered files failed", e);
			}
		}
		else {
			Task<Void> task = new ResetPasswordTask();
			task.setOnScheduled(event -> {
				LOG.debug("Using recovery key to reset password for {}.", vault.getDisplayablePath());
			});
			task.setOnSucceeded(event -> {
				LOG.info("Used recovery key to reset password for {}.", vault.getDisplayablePath());
				window.setScene(recoverResetPasswordSuccessScene.get());
			});
			task.setOnFailed(event -> {
				LOG.error("Resetting password failed.", task.getException());
				appWindows.showErrorWindow(task.getException(), window, null);
			});
			executor.submit(task);
		}
	}

	private class ResetPasswordTask extends Task<Void> {

		private ResetPasswordTask() {
			setOnFailed(event -> LOG.error("Failed to reset password", getException()));
		}

		@Override
		protected Void call() throws IOException, IllegalArgumentException {
			recoveryKeyFactory.newMasterkeyFileWithPassphrase(vault.getPath(), recoveryKey.get(), newPasswordController.passwordField.getCharacters());
			return null;
		}

	}

	/* Getter/Setter */

	public ReadOnlyBooleanProperty passwordSufficientAndMatchingProperty() {
		return newPasswordController.goodPasswordProperty();
	}

	public boolean isPasswordSufficientAndMatching() {
		return newPasswordController.isGoodPassword();
	}

}

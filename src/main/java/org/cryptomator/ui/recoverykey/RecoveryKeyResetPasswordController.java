package org.cryptomator.ui.recoverykey;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import static org.cryptomator.common.Constants.MASTERKEY_FILENAME;

import dagger.Lazy;
import org.cryptomator.common.recovery.CryptoFsInitializer;
import org.cryptomator.common.recovery.MasterkeyService;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.recovery.RecoveryDirectory;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.changepassword.NewPasswordController;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RecoveryKeyScoped
public class RecoveryKeyResetPasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyResetPasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final ExecutorService executor;
	private final StringProperty recoveryKey;
	private final Lazy<Scene> recoverExpertSettingsScene;
	private final FxApplicationWindows appWindows;
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final VaultListManager vaultListManager;
	private final IntegerProperty shorteningThreshold;
	private final ObjectProperty<RecoveryActionType> recoverType;
	private final ObjectProperty<CryptorProvider.Scheme> cipherCombo;
	private final ResourceBundle resourceBundle;
	private final StringProperty buttonText = new SimpleStringProperty();
	private final Dialogs dialogs;
	private final Stage owner;

	public NewPasswordController newPasswordController;

	@Inject
	public RecoveryKeyResetPasswordController(@RecoveryKeyWindow Stage window, //
											  @RecoveryKeyWindow Vault vault, //
											  RecoveryKeyFactory recoveryKeyFactory, //
											  ExecutorService executor, //
											  @Named("keyRecoveryOwner") Stage owner, @RecoveryKeyWindow StringProperty recoveryKey, //
											  @FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS) Lazy<Scene> recoverExpertSettingsScene, //
											  FxApplicationWindows appWindows, //
											  MasterkeyFileAccess masterkeyFileAccess, //
											  VaultListManager vaultListManager, //
											  @Named("shorteningThreshold") IntegerProperty shorteningThreshold, //
											  @Named("recoverType") ObjectProperty<RecoveryActionType> recoverType, //
											  @Named("cipherCombo") ObjectProperty<CryptorProvider.Scheme> cipherCombo,//
											  ResourceBundle resourceBundle, Dialogs dialogs) {
		this.window = window;
		this.vault = vault;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.executor = executor;
		this.recoveryKey = recoveryKey;
		this.recoverExpertSettingsScene = recoverExpertSettingsScene;
		this.appWindows = appWindows;
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.vaultListManager = vaultListManager;
		this.shorteningThreshold = shorteningThreshold;
		this.cipherCombo = cipherCombo;
		this.recoverType = recoverType;
		this.resourceBundle = resourceBundle;
		this.dialogs = dialogs;
		this.owner = owner;
		initButtonText(recoverType.get());
	}

	private void initButtonText(RecoveryActionType type) {
		if (type == RecoveryActionType.RESTORE_MASTERKEY) {
			buttonText.set(resourceBundle.getString("generic.button.close"));
		} else {
			buttonText.set(resourceBundle.getString("generic.button.back"));
		}
	}

	@FXML
	public void close() {
		if (recoverType.getValue().equals(RecoveryActionType.RESTORE_MASTERKEY)) {
			window.close();
		} else {
			window.setScene(recoverExpertSettingsScene.get());
		}
	}
	@FXML
	public void restorePassword() {
		try (RecoveryDirectory recoveryDirectory = RecoveryDirectory.create(vault.getPath())) {
			Path recoveryPath = recoveryDirectory.getRecoveryPath();
			MasterkeyService.recoverFromRecoveryKey(recoveryKey.get(), recoveryKeyFactory, recoveryPath, newPasswordController.passwordField.getCharacters());

			Path masterkeyFilePath = recoveryPath.resolve(MASTERKEY_FILENAME);

			try (Masterkey masterkey = MasterkeyService.load(masterkeyFileAccess, masterkeyFilePath, newPasswordController.passwordField.getCharacters())) {
				CryptoFsInitializer.init(recoveryPath, masterkey, shorteningThreshold, cipherCombo.get());
			}

			recoveryDirectory.moveRecoveredFiles();

			if (!vaultListManager.containsVault(vault.getPath())) {
				vaultListManager.add(vault.getPath());
			}
			window.close();
			dialogs.prepareRecoverPasswordSuccess(window, owner, resourceBundle)
					.setTitleKey("recoveryKey.recoverVaultConfig.title")
					.setMessageKey("recoveryKey.recover.resetVaultConfigSuccess.message")
					.build().showAndWait();

		} catch (IOException | CryptoException e) {
			LOG.error("Recovery process failed", e);
			appWindows.showErrorWindow(e, window, null);
		}
	}

	@FXML
	public void resetPassword() {
		Task<Void> task = new ResetPasswordTask(recoveryKeyFactory, vault, recoveryKey, newPasswordController);

		task.setOnScheduled(_ -> LOG.debug("Using recovery key to reset password for {}.", vault.getDisplayablePath()));

		task.setOnSucceeded(_ -> {
			LOG.info("Used recovery key to reset password for {}.", vault.getDisplayablePath());
			if (vault.getState().equals(org.cryptomator.common.vaults.VaultState.Value.MASTERKEY_MISSING)) {
				dialogs.prepareRecoverPasswordSuccess(window, owner, resourceBundle)
						.setTitleKey("recoveryKey.recoverMasterkey.title")
						.setMessageKey("recoveryKey.recover.resetMasterkeyFileSuccess.message")
						.build().showAndWait();
			} else {
				dialogs.prepareRecoverPasswordSuccess(window, owner, resourceBundle)
						.build().showAndWait();
			}
			window.close();
		});

		task.setOnFailed(_ -> {
			LOG.error("Resetting password failed.", task.getException());
			appWindows.showErrorWindow(task.getException(), window, null);
		});

		executor.submit(task);
	}

	/* Getter/Setter */

	public StringProperty buttonTextProperty() {
		return buttonText;
	}

	public String getButtonText() {
		return buttonText.get();
	}

	public ReadOnlyBooleanProperty passwordSufficientAndMatchingProperty() {
		return newPasswordController.goodPasswordProperty();
	}

	public boolean isPasswordSufficientAndMatching() {
		return newPasswordController.isGoodPassword();
	}
	private final ReadOnlyBooleanWrapper vaultConfigMissing = new ReadOnlyBooleanWrapper();

	public ReadOnlyBooleanProperty vaultConfigMissingProperty() {
		return vaultConfigMissing.getReadOnlyProperty();
	}

	public boolean isVaultConfigMissing() {
		return vault.isMissingVaultConfig();
	}

	private static class ResetPasswordTask extends Task<Void> {

		private static final Logger LOG = LoggerFactory.getLogger(ResetPasswordTask.class);
		private final RecoveryKeyFactory recoveryKeyFactory;
		private final Vault vault;
		private final StringProperty recoveryKey;
		private final NewPasswordController newPasswordController;

		public ResetPasswordTask(RecoveryKeyFactory recoveryKeyFactory, Vault vault, StringProperty recoveryKey, NewPasswordController newPasswordController) {
			this.recoveryKeyFactory = recoveryKeyFactory;
			this.vault = vault;
			this.recoveryKey = recoveryKey;
			this.newPasswordController = newPasswordController;

			setOnFailed(_ -> LOG.error("Failed to reset password", getException()));
		}

		@Override
		protected Void call() throws IOException, IllegalArgumentException {
			recoveryKeyFactory.newMasterkeyFileWithPassphrase(vault.getPath(), recoveryKey.get(), newPasswordController.passwordField.getCharacters());
			return null;
		}
	}
}

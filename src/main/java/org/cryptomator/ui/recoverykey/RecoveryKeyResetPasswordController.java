package org.cryptomator.ui.recoverykey;

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

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

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
	private final Lazy<Scene> recoverExpertSettingsScene;
	private final Lazy<Scene> recoverykeyRecoverScene;
	private final FxApplicationWindows appWindows;
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final VaultListManager vaultListManager;
	private final IntegerProperty shorteningThreshold;
	private final ObjectProperty<RecoveryActionType> recoverType;
	private final ObjectProperty<CryptorProvider.Scheme> cipherCombo;
	private final ResourceBundle resourceBundle;
	private final Dialogs dialogs;

	public NewPasswordController newPasswordController;
	public Button nextButton;

	@Inject
	public RecoveryKeyResetPasswordController(@RecoveryKeyWindow Stage window, //
											  @RecoveryKeyWindow Vault vault, //
											  RecoveryKeyFactory recoveryKeyFactory, //
											  ExecutorService executor, //
											  @RecoveryKeyWindow StringProperty recoveryKey, //
											  @FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS) Lazy<Scene> recoverExpertSettingsScene, //
											  @FxmlScene(FxmlFile.RECOVERYKEY_RECOVER) Lazy<Scene> recoverykeyRecoverScene, //
											  FxApplicationWindows appWindows, //
											  MasterkeyFileAccess masterkeyFileAccess, //
											  VaultListManager vaultListManager, //
											  @Named("shorteningThreshold") IntegerProperty shorteningThreshold, //
											  @Named("recoverType") ObjectProperty<RecoveryActionType> recoverType, //
											  @Named("cipherCombo") ObjectProperty<CryptorProvider.Scheme> cipherCombo, //
											  ResourceBundle resourceBundle, //
											  Dialogs dialogs) {
		this.window = window;
		this.vault = vault;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.executor = executor;
		this.recoveryKey = recoveryKey;
		this.recoverExpertSettingsScene = recoverExpertSettingsScene;
		this.recoverykeyRecoverScene = recoverykeyRecoverScene;
		this.appWindows = appWindows;
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.vaultListManager = vaultListManager;
		this.shorteningThreshold = shorteningThreshold;
		this.cipherCombo = cipherCombo;
		this.recoverType = recoverType;
		this.resourceBundle = resourceBundle;
		this.dialogs = dialogs;
	}

	@FXML
	public void initialize() {
		switch (recoverType.get()) {
			case RESTORE_MASTERKEY, RESTORE_ALL -> nextButton.setText(resourceBundle.getString("recoveryKey.recover.recoverBtn"));
			case RESET_PASSWORD -> nextButton.setText(resourceBundle.getString("recoveryKey.recover.resetBtn"));
			default -> nextButton.setText(resourceBundle.getString("recoveryKey.recover.recoverBtn")); // Fallback
		}
	}

	@FXML
	public void close() {
		switch (recoverType.get()) {
			case RESTORE_ALL -> window.setScene(recoverExpertSettingsScene.get());
			case RESTORE_MASTERKEY, RESET_PASSWORD -> window.setScene(recoverykeyRecoverScene.get());
			default -> window.close();
		}
	}

	@FXML
	public void next() {
		switch (recoverType.get()) {
			case RESTORE_ALL -> restorePassword();
			case RESTORE_MASTERKEY, RESET_PASSWORD -> resetPassword();
			default -> resetPassword(); // Fallback
		}
	}

	@FXML
	public void restorePassword() {
		try (RecoveryDirectory recoveryDirectory = RecoveryDirectory.create(vault.getPath())) {
			Path recoveryPath = recoveryDirectory.getRecoveryPath();
			MasterkeyService.recoverFromRecoveryKey(recoveryKey.get(), recoveryKeyFactory, recoveryPath, newPasswordController.passwordField.getCharacters());

			try (Masterkey masterkey = MasterkeyService.load(masterkeyFileAccess, recoveryPath.resolve(MASTERKEY_FILENAME), newPasswordController.passwordField.getCharacters())) {
				CryptoFsInitializer.init(recoveryPath, masterkey, shorteningThreshold.get(), cipherCombo.get());
			}

			recoveryDirectory.moveRecoveredFile(MASTERKEY_FILENAME);
			recoveryDirectory.moveRecoveredFile(VAULTCONFIG_FILENAME);

			if (!vaultListManager.isAlreadyAdded(vault.getPath())) {
				vaultListManager.add(vault.getPath());
			}
			window.close();
			dialogs.prepareRecoverPasswordSuccess((Stage)window.getOwner()) //
					.setTitleKey("recover.recoverVaultConfig.title") //
					.setMessageKey("recoveryKey.recover.resetVaultConfigSuccess.message") //
					.build().showAndWait();

		} catch (IOException | CryptoException e) {
			LOG.error("Recovery process failed", e);
			appWindows.showErrorWindow(e, window, null);
		}
	}

	@FXML
	public void resetPassword() {
		Task<Void> task = new ResetPasswordTask();

		task.setOnScheduled(_ -> {
			LOG.debug("Using recovery key to reset password for {}.", vault.getDisplayablePath());
		});

		task.setOnSucceeded(_ -> {
			LOG.debug("Used recovery key to reset password for {}.", vault.getDisplayablePath());
			window.close();
			switch (recoverType.get()){
				case RESET_PASSWORD -> dialogs.prepareRecoverPasswordSuccess((Stage)window.getOwner()).build().showAndWait();
				case RESTORE_MASTERKEY -> dialogs.prepareRecoverPasswordSuccess((Stage)window.getOwner()).setTitleKey("recover.recoverMasterkey.title").setMessageKey("recoveryKey.recover.resetMasterkeyFileSuccess.message").build().showAndWait();
				default -> dialogs.prepareRecoverPasswordSuccess(window).build().showAndWait(); // Fallback
			}
		});

		task.setOnFailed(_ -> {
			LOG.error("Resetting password failed.", task.getException());
			appWindows.showErrorWindow(task.getException(), window, null);
		});

		executor.submit(task);
	}

	private class ResetPasswordTask extends Task<Void> {

		private static final Logger LOG = LoggerFactory.getLogger(ResetPasswordTask.class);

		public ResetPasswordTask() {
			setOnFailed(_ -> LOG.error("Failed to reset password", getException()));
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

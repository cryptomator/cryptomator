package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.RecoverUtil;
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
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
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

@RecoveryKeyScoped
public class RecoveryKeyResetPasswordController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyResetPasswordController.class);

	private final Stage window;
	private final Vault vault;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final ExecutorService executor;
	private final StringProperty recoveryKey;
	private final Lazy<Scene> recoverExpertSettingsScene;
	private final Lazy<Scene> recoverResetPasswordSuccessScene;
	private final Lazy<Scene> recoverResetVaultConfigSuccessScene;
	private final FxApplicationWindows appWindows;
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final VaultListManager vaultListManager;
	private final IntegerProperty shorteningThreshold;
	private final ObjectProperty<RecoverUtil.Type> recoverType;
	private final ObjectProperty<CryptorProvider.Scheme> cipherCombo;
	private final ResourceBundle resourceBundle;
	private final StringProperty buttonText = new SimpleStringProperty();

	public NewPasswordController newPasswordController;

	@Inject
	public RecoveryKeyResetPasswordController(@RecoveryKeyWindow Stage window, //
											  @RecoveryKeyWindow Vault vault, //
											  RecoveryKeyFactory recoveryKeyFactory, //
											  ExecutorService executor, //
											  @RecoveryKeyWindow StringProperty recoveryKey, //
											  @FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS) Lazy<Scene> recoverExpertSettingsScene, //
											  @FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD_SUCCESS) Lazy<Scene> recoverResetPasswordSuccessScene, //
											  @FxmlScene(FxmlFile.RECOVERYKEY_RESET_VAULT_CONFIG_SUCCESS) Lazy<Scene> recoverResetVaultConfigSuccessScene, //
											  FxApplicationWindows appWindows, //
											  MasterkeyFileAccess masterkeyFileAccess, //
											  VaultListManager vaultListManager, //
											  @Named("shorteningThreshold") IntegerProperty shorteningThreshold, //
											  @Named("recoverType") ObjectProperty<RecoverUtil.Type> recoverType,
											  @Named("cipherCombo") ObjectProperty<CryptorProvider.Scheme> cipherCombo,//
											  ResourceBundle resourceBundle) {
		this.window = window;
		this.vault = vault;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.executor = executor;
		this.recoveryKey = recoveryKey;
		this.recoverExpertSettingsScene = recoverExpertSettingsScene;
		this.recoverResetPasswordSuccessScene = recoverResetPasswordSuccessScene;
		this.recoverResetVaultConfigSuccessScene = recoverResetVaultConfigSuccessScene;
		this.appWindows = appWindows;
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.vaultListManager = vaultListManager;
		this.shorteningThreshold = shorteningThreshold;
		this.cipherCombo = cipherCombo;
		this.recoverType = recoverType;
		this.resourceBundle = resourceBundle;

		initButtonText(recoverType.get());
	}

	private void initButtonText(RecoverUtil.Type type) {
		if (type == RecoverUtil.Type.RESTORE_MASTERKEY) {
			buttonText.set(resourceBundle.getString("generic.button.close"));
		} else {
			buttonText.set(resourceBundle.getString("generic.button.back"));
		}
	}

	@FXML
	public void close() {
		if(recoverType.getValue().equals(RecoverUtil.Type.RESTORE_MASTERKEY)){
			window.close();
		}
		else {
			window.setScene(recoverExpertSettingsScene.get());
		}
	}

	@FXML
	public void resetPassword() {
		if (vault.isMissingVaultConfig()) {
			try {
				Path recoveryPath = RecoverUtil.createRecoveryDirectory(vault.getPath());
				RecoverUtil.createNewMasterkeyFile(recoveryKeyFactory, recoveryPath, recoveryKey.get(), newPasswordController.passwordField.getCharacters());
				Path masterkeyFilePath = recoveryPath.resolve(MASTERKEY_FILENAME);

				try (Masterkey masterkey = RecoverUtil.loadMasterkey(masterkeyFileAccess, masterkeyFilePath, newPasswordController.passwordField.getCharacters())) {
					RecoverUtil.initializeCryptoFileSystem(recoveryPath,masterkey,shorteningThreshold,cipherCombo.get());
				}

				RecoverUtil.moveRecoveredFiles(recoveryPath, vault.getPath());
				RecoverUtil.deleteRecoveryDirectory(recoveryPath);
				RecoverUtil.addVaultToList(vaultListManager, vault.getPath());

				window.setScene(recoverResetVaultConfigSuccessScene.get());
			} catch (IOException | CryptoException e) {
				LOG.error("Recovery process failed", e);
			}
		} else {
			Task<Void> task = RecoverUtil.createResetPasswordTask( //
					recoveryKeyFactory, //
					vault, //
					recoveryKey, //
					newPasswordController, //
					window, //
					recoverResetPasswordSuccessScene, //
					recoverResetVaultConfigSuccessScene, //
					appWindows);
			executor.submit(task);
		}
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

}

package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.recovery.CryptoFsInitializer;
import org.cryptomator.common.recovery.MasterkeyService;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.recovery.RecoveryDirectory;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.cryptolib.api.CryptoException;
import org.cryptomator.cryptolib.api.InvalidPassphraseException;
import org.cryptomator.cryptolib.api.Masterkey;
import org.cryptomator.cryptolib.common.MasterkeyFileAccess;
import org.cryptomator.ui.common.Animations;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.controls.FormattedLabel;
import org.cryptomator.ui.controls.NiceSecurePasswordField;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
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
public class RecoveryKeyCreationController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyCreationController.class);

	private final Stage window;
	private final Lazy<Scene> successScene;
	private final Lazy<Scene> recoverykeyExpertSettingsScene;
	private final MasterkeyFileAccess masterkeyFileAccess;
	private final Vault vault;
	private final ExecutorService executor;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final StringProperty recoveryKeyProperty;
	private final FxApplicationWindows appWindows;
	public NiceSecurePasswordField passwordField;
	private final IntegerProperty shorteningThreshold;
	private final ObjectProperty<RecoveryActionType> recoverType;
	private final ResourceBundle resourceBundle;
	public FormattedLabel descriptionLabel;
	public Button cancelButton;
	public Button nextButton;
	private final VaultListManager vaultListManager;
	private final Dialogs dialogs;
	private final Stage owner;

	@Inject
	public RecoveryKeyCreationController(FxApplicationWindows appWindows, //
										 @RecoveryKeyWindow Stage window, //
										 @Named("keyRecoveryOwner") Stage owner, //
										 @FxmlScene(FxmlFile.RECOVERYKEY_SUCCESS) Lazy<Scene> successScene, //
										 @FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS) Lazy<Scene> recoverykeyExpertSettingsScene, //
										 @RecoveryKeyWindow Vault vault, //
										 RecoveryKeyFactory recoveryKeyFactory, //
										 MasterkeyFileAccess masterkeyFileAccess, //
										 ExecutorService executor, //
										 @RecoveryKeyWindow StringProperty recoveryKey, //
										 @Named("shorteningThreshold") IntegerProperty shorteningThreshold, //
										 @Named("recoverType") ObjectProperty<RecoveryActionType> recoverType, //
										 VaultListManager vaultListManager, //
										 ResourceBundle resourceBundle, //
										 Dialogs dialogs) {
		this.window = window;
		this.successScene = successScene;
		this.recoverykeyExpertSettingsScene = recoverykeyExpertSettingsScene;
		this.vault = vault;
		this.executor = executor;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.recoveryKeyProperty = recoveryKey;
		this.appWindows = appWindows;
		this.recoverType = recoverType;
		this.resourceBundle = resourceBundle;
		this.masterkeyFileAccess = masterkeyFileAccess;
		this.shorteningThreshold = shorteningThreshold;
		this.vaultListManager = vaultListManager;
		this.owner = owner;
		this.dialogs = dialogs;
	}

	@FXML
	public void initialize() {
		if (recoverType.get() == RecoveryActionType.SHOW_KEY) {
			window.setTitle(resourceBundle.getString("recoveryKey.display.title"));
		} else if (recoverType.get() == RecoveryActionType.RESTORE_VAULT_CONFIG) {
			window.setTitle(resourceBundle.getString("recoveryKey.recoverVaultConfig.title"));
			descriptionLabel.formatProperty().set(resourceBundle.getString("recoveryKey.recover.description"));
			cancelButton.setOnAction((_) -> back());
			cancelButton.setText(resourceBundle.getString("generic.button.back"));
			nextButton.setOnAction((_) -> restoreWithPassword());
		}
	}

	@FXML
	public void back() {
		window.setScene(recoverykeyExpertSettingsScene.get());
		window.centerOnScreen();
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
				appWindows.showErrorWindow(task.getException(), window, window.getScene());
			}
		});
		executor.submit(task);
	}

	@FXML
	public void restoreWithPassword() {

		try (RecoveryDirectory recoveryDirectory = RecoveryDirectory.create(vault.getPath())) {
			Path recoveryPath = recoveryDirectory.getRecoveryPath();

			Path masterkeyFilePath = vault.getPath().resolve(MASTERKEY_FILENAME);

			try (Masterkey masterkey = MasterkeyService.load(masterkeyFileAccess, masterkeyFilePath, passwordField.getCharacters())) {
				var combo = MasterkeyService.detect(masterkey, vault.getPath())
						.orElseThrow(() -> new IllegalStateException("Could not detect combo for vault path: " + vault.getPath()));

				CryptoFsInitializer.init(recoveryPath, masterkey, shorteningThreshold.get(), combo);
			}

			recoveryDirectory.moveRecoveredFile(VAULTCONFIG_FILENAME);

			if (!vaultListManager.isAlreadyAdded(vault.getPath())) {
				vaultListManager.add(vault.getPath());
			}
			window.close();
			dialogs.prepareRecoverPasswordSuccess(window) //
					.setTitleKey("recoveryKey.recoverVaultConfig.title") //
					.setMessageKey("recoveryKey.recover.resetVaultConfigSuccess.message") //
					.setDescriptionKey("recoveryKey.recover.resetMasterkeyFileSuccess.description")
					.build().showAndWait();

		} catch (InvalidPassphraseException e) {
			LOG.info("Password invalid", e);
			Animations.createShakeWindowAnimation(window).play();
		} catch (IOException | CryptoException e) {
			LOG.error("Recovery process failed", e);
			appWindows.showErrorWindow(e, window, null);
		}
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
		protected String call() throws IOException, CryptoException {
			return recoveryKeyFactory.createRecoveryKey(vault.getPath(), passwordField.getCharacters());
		}

	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}
}

package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.util.ResourceBundle;

@RecoveryKeyScoped
public class RecoveryKeyRecoverController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final Lazy<Scene> nextScene;
	private final Lazy<Scene> onBoardingScene;
	private final ResourceBundle resourceBundle;
	public ObjectProperty<RecoveryActionType> recoverType;

	@FXML
	private Button cancelButton;

	@FXML
	RecoveryKeyValidateController recoveryKeyValidateController;

	@Inject
	public RecoveryKeyRecoverController(@RecoveryKeyWindow Stage window, //
										@RecoveryKeyWindow Vault vault, //
										ResourceBundle resourceBundle, //
										@FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD) Lazy<Scene> resetPasswordScene, //
										@FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS) Lazy<Scene> expertSettingsScene, //
										@FxmlScene(FxmlFile.RECOVERYKEY_ONBOARDING) Lazy<Scene> onBoardingScene, //
										@Named("recoverType") ObjectProperty<RecoveryActionType> recoverType) {
		this.window = window;
		this.vault = vault;
		this.resourceBundle = resourceBundle;
		this.onBoardingScene = onBoardingScene;
		this.recoverType = recoverType;
		this.nextScene = switch (recoverType.get()) {
			case RESTORE_ALL, RESTORE_VAULT_CONFIG -> {
				window.setTitle(resourceBundle.getString("recover.recoverVaultConfig.title"));
				yield expertSettingsScene;
			}
			case RESTORE_MASTERKEY -> {
				window.setTitle(resourceBundle.getString("recover.recoverMasterkey.title"));
				yield resetPasswordScene;
			}
			case RESET_PASSWORD -> {
				window.setTitle(resourceBundle.getString("recoveryKey.recover.title"));
				yield resetPasswordScene;
			}
			case SHOW_KEY -> {
				window.setTitle(resourceBundle.getString("recoveryKey.display.title"));
				yield resetPasswordScene;
			}
			default -> throw new IllegalArgumentException("Unexpected recovery action type: " + recoverType.get());
		};
	}

	@FXML
	public void initialize() {
		if (recoverType.get() == RecoveryActionType.RESET_PASSWORD) {
			cancelButton.setText(resourceBundle.getString("generic.button.cancel"));
		} else {
			cancelButton.setText(resourceBundle.getString("generic.button.back"));
		}
	}

	@FXML
	public void closeOrReturn() {
		switch (recoverType.get()) {
			case RESET_PASSWORD -> window.close();
			case RESTORE_MASTERKEY -> {
				window.setScene(onBoardingScene.get());
				window.centerOnScreen();
			}
			default -> {
				if(vault.getState().equals(VaultState.Value.ALL_MISSING)){
					recoverType.set(RecoveryActionType.RESTORE_ALL);
				}
				else {
					recoverType.set(RecoveryActionType.RESTORE_VAULT_CONFIG);
				}
				window.setScene(onBoardingScene.get());
				window.centerOnScreen();
			}
		}
	}

	@FXML
	public void recover() {
		window.setScene(nextScene.get());
	}

	/* Getter/Setter */

	public RecoveryKeyValidateController getValidateController() {
		return recoveryKeyValidateController;
	}

}

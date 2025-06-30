package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.recovery.RecoveryActionType;
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
										@FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD) Lazy<Scene> resetPasswordScene, //
										@FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS) Lazy<Scene> expertSettingsScene, //
										@FxmlScene(FxmlFile.RECOVERYKEY_ONBOARDING) Lazy<Scene> onBoardingScene, //
										ResourceBundle resourceBundle, //
										@Named("recoverType") ObjectProperty<RecoveryActionType> recoverType) {
		this.window = window;
		this.recoverType = recoverType;
		this.onBoardingScene = onBoardingScene;
		this.resourceBundle = resourceBundle;
		this.nextScene = switch (recoverType.get()) {
			case RESTORE_ALL, RESTORE_VAULT_CONFIG -> {
				window.setTitle(resourceBundle.getString("recoveryKey.recoverVaultConfig.title"));
				yield expertSettingsScene;
			}
			case RESTORE_MASTERKEY -> {
				window.setTitle(resourceBundle.getString("recoveryKey.recoverMasterkey.title"));
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
			default -> {
				yield null;
			}
		};

	}

	@FXML
	public void initialize() {
		switch (recoverType.get()) {
			case RESTORE_ALL, RESTORE_VAULT_CONFIG -> cancelButton.setText(resourceBundle.getString("generic.button.back"));
			case RESET_PASSWORD -> cancelButton.setText(resourceBundle.getString("generic.button.cancel"));
		}
	}

	@FXML
	public void close() {
		switch (recoverType.get()) {
			case RESTORE_ALL, RESTORE_VAULT_CONFIG -> window.setScene(onBoardingScene.get());
			case RESET_PASSWORD -> window.close();
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

package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.RecoverUtil;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.ResourceBundle;

@RecoveryKeyScoped
public class RecoveryKeyRecoverController implements FxController {

	private final Stage window;
	private final Lazy<Scene> nextScene;

	@FXML
	RecoveryKeyValidateController recoveryKeyValidateController;

	@Inject
	public RecoveryKeyRecoverController(@RecoveryKeyWindow Stage window, //
										@FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD) Lazy<Scene> resetPasswordScene, //
										@FxmlScene(FxmlFile.RECOVERYKEY_EXPERT_SETTINGS) Lazy<Scene> expertSettingsScene, //
										ResourceBundle resourceBundle,
										@Named("recoverType") ObjectProperty<RecoverUtil.Type> recoverType) {
		this.window = window;

		this.nextScene = switch (recoverType.get()) {
			case RESTORE_VAULT_CONFIG -> {
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
			case SHOW_KEY-> {
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
	}

	@FXML
	public void close() {
		window.close();
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

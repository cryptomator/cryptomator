package org.cryptomator.ui.recoverykey;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.ResourceBundle;

import dagger.Lazy;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

@RecoveryKeyScoped
public class RecoveryKeyIsHubVaultController implements FxController {

	private final Stage window;
	private final Lazy<Scene> recoverykeyRecoverScene;
	private final ObjectProperty<RecoveryActionType> recoverType;

	@Inject
	public RecoveryKeyIsHubVaultController(@RecoveryKeyWindow Stage window, //
										   @FxmlScene(FxmlFile.RECOVERYKEY_RECOVER) Lazy<Scene> recoverykeyRecoverScene, //
										   @Named("recoverType") ObjectProperty<RecoveryActionType> recoverType, //
										   ResourceBundle resourceBundle) {
		this.window = window;
		window.setTitle(resourceBundle.getString("recoveryKey.recoverVaultConfig.title"));

		this.recoverykeyRecoverScene = recoverykeyRecoverScene;
		this.recoverType = recoverType;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void recover() {
		recoverType.set(RecoveryActionType.RESTORE_VAULT_CONFIG);
		window.setScene(recoverykeyRecoverScene.get());
	}
}

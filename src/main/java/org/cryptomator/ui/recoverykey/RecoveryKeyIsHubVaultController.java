package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.RecoverUtil;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.ResourceBundle;

@RecoveryKeyScoped
public class RecoveryKeyIsHubVaultController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyIsHubVaultController.class);

	private final Stage window;
	private final Lazy<Scene> recoverykeyRecoverScene;
	private final ObjectProperty<RecoverUtil.Type> recoverType;
	private final ResourceBundle resourceBundle;

	@Inject
	public RecoveryKeyIsHubVaultController(@RecoveryKeyWindow Stage window,
										   @FxmlScene(FxmlFile.RECOVERYKEY_RECOVER) Lazy<Scene> recoverykeyRecoverScene,
										   @Named("recoverType") ObjectProperty<RecoverUtil.Type> recoverType,
										   ResourceBundle resourceBundle) {
		this.window = window;
		window.setTitle(resourceBundle.getString("recoveryKey.recoverVaultConfig.title"));

		this.recoverykeyRecoverScene = recoverykeyRecoverScene;
		this.recoverType = recoverType;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void recover() {
		recoverType.set(RecoverUtil.Type.RESTORE_VAULT_CONFIG);
		window.setScene(recoverykeyRecoverScene.get());
	}
}

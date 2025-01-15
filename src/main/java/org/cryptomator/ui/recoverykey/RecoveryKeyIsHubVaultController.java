package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.ResourceBundle;

@RecoveryKeyScoped
public class RecoveryKeyIsHubVaultController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyIsHubVaultController.class);

	private final Stage window;
	private final Lazy<Scene> recoverykeyRecoverScene;

	@Inject
	public RecoveryKeyIsHubVaultController(@RecoveryKeyWindow Stage window,
										   @RecoveryKeyWindow Vault vault,
										   @RecoveryKeyWindow StringProperty recoveryKey,
										   @FxmlScene(FxmlFile.RECOVERYKEY_RECOVER) Lazy<Scene> recoverykeyRecoverScene,
										   ResourceBundle resourceBundle) {
		this.window = window;
		//window.setTitle("Is it a hub vault? Huh?");
		this.recoverykeyRecoverScene = recoverykeyRecoverScene;
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
		window.setScene(recoverykeyRecoverScene.get());
	}
}

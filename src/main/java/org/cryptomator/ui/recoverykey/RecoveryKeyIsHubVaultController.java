package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

@RecoveryKeyScoped
public class RecoveryKeyIsHubVaultController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyIsHubVaultController.class);

	private final Stage window;
	private final Lazy<Scene> recoverykeyRecoverScene;

	@Inject
	public RecoveryKeyIsHubVaultController(@RecoveryKeyWindow Stage window,
										   @FxmlScene(FxmlFile.RECOVERYKEY_RECOVER) Lazy<Scene> recoverykeyRecoverScene) {
		this.window = window;
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
		window.setTitle("Recover Config");
		window.setScene(recoverykeyRecoverScene.get());
	}
}

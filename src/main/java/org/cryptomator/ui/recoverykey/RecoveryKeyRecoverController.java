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
import java.util.ResourceBundle;

@RecoveryKeyScoped
public class RecoveryKeyRecoverController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RecoveryKeyCreationController.class);

	private final Stage window;
	private final Lazy<Scene> resetPasswordScene;

	@FXML
	RecoveryKeyValidateController recoveryKeyValidateController;

	@Inject
	public RecoveryKeyRecoverController(@RecoveryKeyWindow Stage window, //
										@FxmlScene(FxmlFile.RECOVERYKEY_RESET_PASSWORD) Lazy<Scene> resetPasswordScene, //
										ResourceBundle resourceBundle) {
		this.window = window;
		window.setTitle(resourceBundle.getString("recoveryKey.recover.title"));
		this.resetPasswordScene = resetPasswordScene;
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
		window.setScene(resetPasswordScene.get());
	}

	/* Getter/Setter */

	public RecoveryKeyValidateController getValidateController() {
		return recoveryKeyValidateController;
	}

}

package org.cryptomator.ui.recoverykey;

import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
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
										ResourceBundle resourceBundle) {
		this.window = window;
		if (window.getTitle().equals("Recover Config")) {
			this.nextScene = expertSettingsScene;
		} else if (window.getTitle().equals(resourceBundle.getString("recoveryKey.recover.title"))) {
			this.nextScene = resetPasswordScene;
		} else {
			this.nextScene = resetPasswordScene;
		}
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

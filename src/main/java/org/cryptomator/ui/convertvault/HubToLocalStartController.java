package org.cryptomator.ui.convertvault;

import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.recoverykey.RecoveryKeyValidateController;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HubToLocalStartController implements FxController {

	private final Stage window;
	private final Lazy<Scene> convertScene;

	@FXML
	RecoveryKeyValidateController recoveryKeyValidateController;

	@Inject
	public HubToLocalStartController(@ConvertVaultWindow Stage window, @FxmlScene(FxmlFile.CONVERTVAULT_HUBTOLOCAL_CONVERT) Lazy<Scene> convertScene) {
		this.window = window;
		this.convertScene = convertScene;
	}

	@FXML
	public void initialize() {
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void next() {
		window.setScene(convertScene.get());
	}

	/* Getter/Setter */

	public RecoveryKeyValidateController getValidateController() {
		return recoveryKeyValidateController;
	}
}

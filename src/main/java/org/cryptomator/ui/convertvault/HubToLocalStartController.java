package org.cryptomator.ui.convertvault;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.recoverykey.RecoveryKeyValidateController;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class HubToLocalStartController implements FxController {

	private final Stage window;

	@FXML
	RecoveryKeyValidateController recoveryKeyValidateController;

	@Inject
	public HubToLocalStartController(@ConvertVaultWindow Stage window) {
		this.window = window;
	}

	@FXML
	public void initialize() {
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void convert() {
		//window.setScene(resetPasswordScene.get());
	}

	/* Getter/Setter */

	public RecoveryKeyValidateController getValidateController() {
		return recoveryKeyValidateController;
	}
}

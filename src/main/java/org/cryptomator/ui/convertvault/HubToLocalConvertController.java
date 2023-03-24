package org.cryptomator.ui.convertvault;

import org.cryptomator.ui.changepassword.NewPasswordController;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class HubToLocalConvertController implements FxController {

	private final Stage window;

	@FXML
	NewPasswordController newPasswordController;

	@Inject
	public HubToLocalConvertController(@ConvertVaultWindow Stage window) {
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

	public NewPasswordController getNewPasswordController() {
		return newPasswordController;
	}
}

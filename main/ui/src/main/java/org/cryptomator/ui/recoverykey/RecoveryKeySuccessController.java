package org.cryptomator.ui.recoverykey;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@RecoveryKeyScoped
public class RecoveryKeySuccessController implements FxController {

	private final Stage window;

	@Inject
	public RecoveryKeySuccessController(@RecoveryKeyWindow Stage window) {
		this.window = window;
	}

	@FXML
	public void close() {
		window.close();
	}

}

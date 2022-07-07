package org.cryptomator.ui.recoverykey;

import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@RecoveryKeyScoped
public class RecoveryKeyResetPasswordSuccessController  implements FxController {

	private final Stage window;

	@Inject
	public RecoveryKeyResetPasswordSuccessController(@RecoveryKeyWindow Stage window) {
		this.window = window;
	}

	@FXML
	public void close() {
		window.close();
	}

}

package org.cryptomator.ui.recoverykey;

import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@RecoveryKeyScoped
public class RecoveryKeyResetPasswordSuccessController implements FxController {

	private final Stage window;
	private final Stage owner;

	@Inject
	public RecoveryKeyResetPasswordSuccessController(@RecoveryKeyWindow Stage window, //
													 @Named("keyRecoveryOwner") Stage owner) {

		this.window = window;
		this.owner = owner;
	}

	@FXML
	public void close() {
		if (!owner.getTitle().equals("Cryptomator")) {
			owner.close();
		}
		window.close();
	}

}

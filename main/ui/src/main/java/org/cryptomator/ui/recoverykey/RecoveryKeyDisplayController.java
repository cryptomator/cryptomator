package org.cryptomator.ui.recoverykey;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@RecoveryKeyScoped
public class RecoveryKeyDisplayController implements FxController {

	private final Stage window;
	private final StringProperty recoveryKeyProperty;

	@Inject
	public RecoveryKeyDisplayController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow StringProperty recoveryKey) {
		this.window = window;
		this.recoveryKeyProperty = recoveryKey;
	}

	@FXML
	public void close() {
		window.close();
	}

	/* Getter/Setter */

	public ReadOnlyStringProperty recoveryKeyProperty() {
		return recoveryKeyProperty;
	}

	public String getRecoveryKey() {
		return recoveryKeyProperty.get();
	}
}

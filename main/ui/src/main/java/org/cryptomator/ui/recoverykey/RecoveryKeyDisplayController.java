package org.cryptomator.ui.recoverykey;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@RecoveryKeyScoped
public class RecoveryKeyDisplayController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final StringProperty recoveryKeyProperty;

	@Inject
	public RecoveryKeyDisplayController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, @RecoveryKeyWindow StringProperty recoveryKey) {
		this.window = window;
		this.vault = vault;
		this.recoveryKeyProperty = recoveryKey;
	}

	@FXML
	public void close() {
		window.close();
	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public ReadOnlyStringProperty recoveryKeyProperty() {
		return recoveryKeyProperty;
	}

	public String getRecoveryKey() {
		return recoveryKeyProperty.get();
	}
}

package org.cryptomator.ui.recoverykey;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@RecoveryKeyScoped
public class RecoveryKeyRecoverController implements FxController {

	private final Stage window;
	private final Vault vault;
	private final StringProperty recoveryKey;
	private final RecoveryKeyFactory recoveryKeyFactory;
	private final BooleanBinding validRecoveryKey;

	public TextArea textarea;

	@Inject
	public RecoveryKeyRecoverController(@RecoveryKeyWindow Stage window, @RecoveryKeyWindow Vault vault, @RecoveryKeyWindow StringProperty recoveryKey, RecoveryKeyFactory recoveryKeyFactory) {
		this.window = window;
		this.vault = vault;
		this.recoveryKey = recoveryKey;
		this.recoveryKeyFactory = recoveryKeyFactory;
		this.validRecoveryKey = Bindings.createBooleanBinding(this::isValidRecoveryKey, recoveryKey);
	}

	@FXML
	public void initialize() {
		textarea.textProperty().bindBidirectional(recoveryKey);
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void recover() {
		recoveryKeyFactory.validateRecoveryKey(textarea.getText());

	}

	/* Getter/Setter */

	public Vault getVault() {
		return vault;
	}

	public BooleanBinding validRecoveryKeyProperty() {
		return validRecoveryKey;
	}

	public boolean isValidRecoveryKey() {
		return recoveryKeyFactory.validateRecoveryKey(recoveryKey.get());
	}
}

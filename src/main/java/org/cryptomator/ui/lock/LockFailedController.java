package org.cryptomator.ui.lock;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@LockScoped
public class LockFailedController implements FxController {

	private final Stage window;
	private final Vault vault;

	@Inject
	public LockFailedController(@LockWindow Stage window, @LockWindow Vault vault) {
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void close() {
		window.close();
	}

	// ----- Getter & Setter -----
	public String getVaultName() {
		return vault.getDisplayName();
	}
}

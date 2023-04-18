package org.cryptomator.ui.convertvault;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class HubToPasswordSuccessController implements FxController {

	private final Stage window;
	private final Vault vault;

	@Inject
	HubToPasswordSuccessController(@ConvertVaultWindow Stage window, @ConvertVaultWindow Vault vault) {
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void close() {
		window.close();
		window.getOwner().hide();
	}

	/* Observables */

	public Vault getVault() {
		return vault;
	}
}

package org.cryptomator.ui.convertvault;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class HubToLocalSuccessController implements FxController {

	private final Stage window;
	private final Vault vault;

	@Inject
	HubToLocalSuccessController(@ConvertVaultWindow Stage window, @ConvertVaultWindow Vault vault) {
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

package org.cryptomator.ui.convertvault;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class HubToLocalSuccessController implements FxController {

	private final FxApplicationWindows appWindows;
	private final Stage window;
	private final Vault vault;

	@Inject
	HubToLocalSuccessController(FxApplicationWindows appWindows, @ConvertVaultWindow Stage window, @ConvertVaultWindow Vault vault) {
		this.appWindows = appWindows;
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void unlockAndClose() {
		close();
		vault.getVaultConfigCache();
		appWindows.startUnlockWorkflow(vault, window);
	}

	@FXML
	public void close() {
		window.close();
	}

	/* Observables */

	public Vault getVault() {
		return vault;
	}
}

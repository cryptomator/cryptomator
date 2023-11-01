package org.cryptomator.ui.unlock;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@UnlockScoped
public class UnlockFuseRestartRequiredController implements FxController {

	private final Stage window;
	private final FxApplicationWindows appWindows;
	private final Vault vault;
	@Inject
	UnlockFuseRestartRequiredController(@UnlockWindow Stage window,
			FxApplicationWindows appWindows,
			@UnlockWindow Vault vault) {
		this.window = window;
		this.appWindows = appWindows;
		this.vault = vault;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void closeAndOpenVaultOptions() {
		appWindows.showVaultOptionsWindow(vault, SelectedVaultOptionsTab.MOUNT);
		window.close();
	}

}
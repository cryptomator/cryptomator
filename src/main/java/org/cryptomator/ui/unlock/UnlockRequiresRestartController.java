package org.cryptomator.ui.unlock;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.ResourceBundle;

@UnlockScoped
public class UnlockRequiresRestartController implements FxController {

	private final Stage window;
	private final ResourceBundle resourceBundle;
	private final FxApplicationWindows appWindows;
	private final Vault vault;

	@Inject
	UnlockRequiresRestartController(@UnlockWindow Stage window, //
									ResourceBundle resourceBundle, //
									FxApplicationWindows appWindows, //
									@UnlockWindow Vault vault) {
		this.window = window;
		this.resourceBundle = resourceBundle;
		this.appWindows = appWindows;
		this.vault = vault;
	}

	public void initialize() {
		window.setTitle(String.format(resourceBundle.getString("unlock.error.title"), vault.getDisplayName()));
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
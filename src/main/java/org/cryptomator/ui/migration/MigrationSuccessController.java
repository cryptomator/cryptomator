package org.cryptomator.ui.migration;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.fxapp.PrimaryStage;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@MigrationScoped
public class MigrationSuccessController implements FxController {

	private final FxApplicationWindows appWindows;
	private final Stage window;
	private final Vault vault;
	private final Stage mainWindow;

	@Inject
	MigrationSuccessController(FxApplicationWindows appWindows, @MigrationWindow Stage window, @MigrationWindow Vault vault, @PrimaryStage Stage mainWindow) {
		this.appWindows = appWindows;
		this.window = window;
		this.vault = vault;
		this.mainWindow = mainWindow;
	}

	@FXML
	public void unlockAndClose() {
		close();
		appWindows.startUnlockWorkflow(vault, mainWindow);
	}

	@FXML
	public void close() {
		window.close();
	}

	/* Getter/Setters */

	public Vault getVault() {
		return vault;
	}

}

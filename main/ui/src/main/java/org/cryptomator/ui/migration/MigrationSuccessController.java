package org.cryptomator.ui.migration;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;

import javax.inject.Inject;

@MigrationScoped
public class MigrationSuccessController implements FxController {

	private final FxApplication fxApplication;
	private final Stage window;
	private final Vault vault;

	@Inject
	MigrationSuccessController(FxApplication fxApplication, @MigrationWindow Stage window, @MigrationWindow Vault vault) {
		this.fxApplication = fxApplication;
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void unlockAndClose() {
		close();
		fxApplication.startUnlockWorkflow(vault);
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

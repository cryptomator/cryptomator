package org.cryptomator.ui.migration;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.mainwindow.MainWindow;

import javax.inject.Inject;
import java.util.Optional;

@MigrationScoped
public class MigrationSuccessController implements FxController {

	private final FxApplication fxApplication;
	private final Stage window;
	private final Vault vault;
	private final Stage mainWindow;

	@Inject
	MigrationSuccessController(FxApplication fxApplication, @MigrationWindow Stage window, @MigrationWindow Vault vault, @MainWindow Stage mainWindow) {
		this.fxApplication = fxApplication;
		this.window = window;
		this.vault = vault;
		this.mainWindow = mainWindow;
	}

	@FXML
	public void unlockAndClose() {
		close();
		fxApplication.startUnlockWorkflow(vault, Optional.of(mainWindow));
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

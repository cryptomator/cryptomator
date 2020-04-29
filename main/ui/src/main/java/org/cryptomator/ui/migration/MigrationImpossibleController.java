package org.cryptomator.ui.migration;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;

import javax.inject.Inject;

public class MigrationImpossibleController implements FxController {

	private static final String HELP_URI = "https://docs.cryptomator.org/en/1.5/help/manual-migration/";

	private final FxApplication fxApplication;
	private final Stage window;
	private final Vault vault;

	@Inject
	MigrationImpossibleController(FxApplication fxApplication, @MigrationWindow Stage window, @MigrationWindow Vault vault) {
		this.fxApplication = fxApplication;
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void getMigrationHelp() {
		fxApplication.getHostServices().showDocument(HELP_URI);
	}

	/* Getter/Setters */

	public Vault getVault() {
		return vault;
	}

	public String getHelpUri() {
		return HELP_URI;
	}

}

package org.cryptomator.ui.migration;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class MigrationImpossibleController implements FxController {

	private static final String HELP_URI = "https://docs.cryptomator.org/help/manual-migration/";

	private final Application application;
	private final Stage window;
	private final Vault vault;

	@Inject
	MigrationImpossibleController(Application application, @MigrationWindow Stage window, @MigrationWindow Vault vault) {
		this.application = application;
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void getMigrationHelp() {
		application.getHostServices().showDocument(HELP_URI);
	}

	/* Getter/Setters */

	public Vault getVault() {
		return vault;
	}

	public String getHelpUri() {
		return HELP_URI;
	}

}

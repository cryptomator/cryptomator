package org.cryptomator.ui.migration;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.net.URI;

public class MigrationImpossibleController implements FxController {

	private static final String HELP_PATH = "help/manual-migration/";

	private final Application application;
	private final Stage window;
	private final Vault vault;
	private final URI helpUri;

	@Inject
	MigrationImpossibleController(Application application, @MigrationWindow Stage window, @MigrationWindow Vault vault, @Named("docUri") URI docUri) {
		this.application = application;
		this.window = window;
		this.vault = vault;
		this.helpUri = docUri.resolve(HELP_PATH);
	}

	@FXML
	public void close() {
		window.close();
	}

	@FXML
	public void getMigrationHelp() {
		application.getHostServices().showDocument(helpUri.toString());
	}

	/* Getter/Setters */

	public Vault getVault() {
		return vault;
	}

	public String getHelpUri() {
		return helpUri.toString();
	}

}

package org.cryptomator.ui.importtemplate;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@ImportTemplateScoped
public class ImportTemplateSuccessController implements FxController {

	private final Stage window;
	private final ReadOnlyObjectProperty<Vault> vault;

	@Inject
	ImportTemplateSuccessController(@ImportTemplateWindow Stage window, @ImportTemplateWindow ObjectProperty<Vault> vault) {
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void close() {
		window.close();
	}

	// TODO (import-template step 4b): unlockAndClose() -> appWindows.startUnlockWorkflow(vault.get(), window)

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

}

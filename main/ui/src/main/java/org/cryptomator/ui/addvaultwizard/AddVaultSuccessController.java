package org.cryptomator.ui.addvaultwizard;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;

import javax.inject.Inject;

@AddVaultWizardScoped
public class AddVaultSuccessController implements FxController {

	private final FxApplication fxApplication;
	private final Stage window;
	private final ReadOnlyObjectProperty<Vault> vault;

	@Inject
	AddVaultSuccessController(FxApplication fxApplication, @AddVaultWizard Stage window, @AddVaultWizard ObjectProperty<Vault> vault) {
		this.fxApplication = fxApplication;
		this.window = window;
		this.vault = vault;
	}

	public void unlockAndClose(ActionEvent actionEvent) {
		close(actionEvent);
		fxApplication.showUnlockWindow(vault.get());
	}

	@FXML
	public void close(ActionEvent actionEvent) {
		window.close();
	}

	/* Observables */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}
}

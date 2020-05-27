package org.cryptomator.ui.addvaultwizard;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;

import javax.inject.Inject;
import java.util.Optional;

@AddVaultWizardScoped
public class AddVaultSuccessController implements FxController {

	private final FxApplication fxApplication;
	private final Stage window;
	private final ReadOnlyObjectProperty<Vault> vault;

	@Inject
	AddVaultSuccessController(FxApplication fxApplication, @AddVaultWizardWindow Stage window, @AddVaultWizardWindow ObjectProperty<Vault> vault) {
		this.fxApplication = fxApplication;
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void unlockAndClose() {
		close();
		fxApplication.startUnlockWorkflow(vault.get(), Optional.of(window));
	}

	@FXML
	public void close() {
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

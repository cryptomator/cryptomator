package org.cryptomator.ui.addvaultwizard;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@AddVaultWizardScoped
public class AddVaultSuccessController implements FxController {

	private final FxApplicationWindows appWindows;
	private final Stage window;
	private final ReadOnlyObjectProperty<Vault> vault;

	@Inject
	AddVaultSuccessController(FxApplicationWindows appWindows, @AddVaultWizardWindow Stage window, @AddVaultWizardWindow ObjectProperty<Vault> vault) {
		this.appWindows = appWindows;
		this.window = window;
		this.vault = vault;
	}

	@FXML
	public void unlockAndClose() {
		close();
		appWindows.startUnlockWorkflow(vault.get(), window);
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

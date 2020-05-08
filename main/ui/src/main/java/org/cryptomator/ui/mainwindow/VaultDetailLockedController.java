package org.cryptomator.ui.mainwindow;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;

import javax.inject.Inject;

@MainWindowScoped
public class VaultDetailLockedController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final FxApplication application;
	private final VaultOptionsComponent.Builder vaultOptionsWindow;

	@Inject
	VaultDetailLockedController(ObjectProperty<Vault> vault, FxApplication application, VaultOptionsComponent.Builder vaultOptionsWindow) {
		this.vault = vault;
		this.application = application;
		this.vaultOptionsWindow = vaultOptionsWindow;
	}

	@FXML
	public void unlock() {
		application.startUnlockWorkflow(vault.get());
	}

	@FXML
	public void showVaultOptions() {
		vaultOptionsWindow.vault(vault.get()).build().showVaultOptionsWindow();
	}
	
	/* Getter/Setter */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}
}

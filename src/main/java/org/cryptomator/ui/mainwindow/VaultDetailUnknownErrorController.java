package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.removevault.RemoveVaultComponent;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@MainWindowScoped
public class VaultDetailUnknownErrorController implements FxController {

	private final ObjectProperty<Vault> vault;
	private final FxApplicationWindows appWindows;
	private final Stage errorWindow;
	private final RemoveVaultComponent.Builder removeVault;

	@Inject
	public VaultDetailUnknownErrorController(ObjectProperty<Vault> vault, FxApplicationWindows appWindows, @Named("errorWindow") Stage errorWindow, RemoveVaultComponent.Builder removeVault) {
		this.vault = vault;
		this.appWindows = appWindows;
		this.errorWindow = errorWindow;
		this.removeVault = removeVault;
	}

	@FXML
	public void showError() {
		appWindows.showErrorWindow(vault.get().getLastKnownException(), errorWindow, null);
	}

	@FXML
	public void reload() {
		VaultListManager.redetermineVaultState(vault.get());
	}

	@FXML
	void didClickRemoveVault() {
		removeVault.vault(vault.get()).build().showRemoveVault();
	}
}

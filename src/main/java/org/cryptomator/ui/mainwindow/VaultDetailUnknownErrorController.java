package org.cryptomator.ui.mainwindow;

import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.removevault.RemoveVaultComponent;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@MainWindowScoped
public class VaultDetailUnknownErrorController implements FxController {

	private final ObjectProperty<Vault> vault;
	private final ErrorComponent.Builder errorComponentBuilder;
	private final Stage errorWindow;
	private final RemoveVaultComponent.Builder removeVault;

	@Inject
	public VaultDetailUnknownErrorController(ObjectProperty<Vault> vault, ErrorComponent.Builder errorComponentBuilder, @Named("errorWindow") Stage errorWindow, RemoveVaultComponent.Builder removeVault) {
		this.vault = vault;
		this.errorComponentBuilder = errorComponentBuilder;
		this.errorWindow = errorWindow;
		this.removeVault = removeVault;
	}

	@FXML
	public void showError() {
		errorComponentBuilder.window(errorWindow).cause(vault.get().getLastKnownException()).build().showErrorScene();
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

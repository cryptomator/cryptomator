package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@MainWindowScoped
public class VaultDetailLockedController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final FxApplicationWindows appWindows;
	private final VaultOptionsComponent.Factory vaultOptionsWindow;
	private final Stage mainWindow;
	private final ObservableValue<Boolean> passwordSaved;

	@Inject
	VaultDetailLockedController(ObjectProperty<Vault> vault, FxApplicationWindows appWindows, VaultOptionsComponent.Factory vaultOptionsWindow, KeychainManager keychain, @MainWindow Stage mainWindow) {
		this.vault = vault;
		this.appWindows = appWindows;
		this.vaultOptionsWindow = vaultOptionsWindow;
		this.mainWindow = mainWindow;
		this.passwordSaved = Bindings.createBooleanBinding(() -> {
			var v = vault.get();
			return v != null && keychain.getPassphraseStoredProperty(v.getId()).getValue();
		}, vault, keychain.getKeychainImplementation());
	}

	@FXML
	public void unlock() {
		appWindows.startUnlockWorkflow(vault.get(), mainWindow);
	}

	@FXML
	public void share() {
		appWindows.showShareVaultWindow(vault.get());
	}

	@FXML
	public void showVaultOptions() {
		vaultOptionsWindow.create(vault.get()).showVaultOptionsWindow(SelectedVaultOptionsTab.ANY);
	}

	@FXML
	public void showKeyVaultOptions() {
		vaultOptionsWindow.create(vault.get()).showVaultOptionsWindow(SelectedVaultOptionsTab.KEY);
	}

	/* Getter/Setter */

	public ReadOnlyObjectProperty<Vault> vaultProperty() {
		return vault;
	}

	public Vault getVault() {
		return vault.get();
	}

	public ObservableValue<Boolean> passwordSavedProperty() {
		return passwordSaved;
	}

	public boolean isPasswordSaved() {
		return passwordSaved.getValue();
	}
}

package org.cryptomator.ui.mainwindow;

import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;

import javax.inject.Inject;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@MainWindowScoped
public class VaultDetailLockedController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final FxApplicationWindows appWindows;
	private final VaultOptionsComponent.Factory vaultOptionsWindow;
	private final KeychainManager keychain;
	private final Stage mainWindow;
	private final BooleanExpression passwordSaved;

	@Inject
	VaultDetailLockedController(ObjectProperty<Vault> vault, FxApplicationWindows appWindows, VaultOptionsComponent.Factory vaultOptionsWindow, KeychainManager keychain, @MainWindow Stage mainWindow) {
		this.vault = vault;
		this.appWindows = appWindows;
		this.vaultOptionsWindow = vaultOptionsWindow;
		this.keychain = keychain;
		this.mainWindow = mainWindow;
		if (keychain.isSupported() && !keychain.isLocked()) {
			this.passwordSaved = BooleanExpression.booleanExpression(EasyBind.select(vault).selectObject(v -> keychain.getPassphraseStoredProperty(v.getId())));
		} else {
			this.passwordSaved = new SimpleBooleanProperty(false);
		}
	}

	@FXML
	public void unlock() {
		appWindows.startUnlockWorkflow(vault.get(), mainWindow);
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

	public BooleanExpression passwordSavedProperty() {
		return passwordSaved;
	}

	public boolean isPasswordSaved() {
		if (keychain.isSupported() && vault.get() != null) {
			return keychain.getPassphraseStoredProperty(vault.get().getId()).get();
		} else return false;
	}
}

package org.cryptomator.ui.mainwindow;

import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.health.HealthCheckComponent;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;

import javax.inject.Inject;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.Optional;

@MainWindowScoped
public class VaultDetailLockedController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final FxApplication application;
	private final VaultOptionsComponent.Builder vaultOptionsWindow;
	private final KeychainManager keychain;
	private final Stage mainWindow;
	private final BooleanExpression passwordSaved;

	@Inject
	VaultDetailLockedController(ObjectProperty<Vault> vault, FxApplication application,  VaultOptionsComponent.Builder vaultOptionsWindow, KeychainManager keychain, @MainWindow Stage mainWindow) {
		this.vault = vault;
		this.application = application;
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
		application.startUnlockWorkflow(vault.get(), Optional.of(mainWindow));
	}

	@FXML
	public void showVaultOptions() {
		vaultOptionsWindow.vault(vault.get()).build().showVaultOptionsWindow(SelectedVaultOptionsTab.ANY);
	}

	@FXML
	public void showKeyVaultOptions() {
		vaultOptionsWindow.vault(vault.get()).build().showVaultOptionsWindow(SelectedVaultOptionsTab.KEY);
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

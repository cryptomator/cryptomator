package org.cryptomator.ui.mainwindow;

import com.tobiasdiez.easybind.EasyBind;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.keychain.KeychainManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;

import javax.inject.Inject;
import java.util.Optional;

@MainWindowScoped
public class VaultDetailLockedController implements FxController {

	private final ReadOnlyObjectProperty<Vault> vault;
	private final FxApplication application;
	private final VaultOptionsComponent.Builder vaultOptionsWindow;
	private final Optional<KeychainManager> keychainManagerOptional;
	private final Stage mainWindow;
	private final BooleanExpression passwordSaved;

	@Inject
	VaultDetailLockedController(ObjectProperty<Vault> vault, FxApplication application, VaultOptionsComponent.Builder vaultOptionsWindow, Optional<KeychainManager> keychainManagerOptional, @MainWindow Stage mainWindow) {
		this.vault = vault;
		this.application = application;
		this.vaultOptionsWindow = vaultOptionsWindow;
		this.keychainManagerOptional = keychainManagerOptional;
		this.mainWindow = mainWindow;
		if (keychainManagerOptional.isPresent()) {
			this.passwordSaved = BooleanExpression.booleanExpression(EasyBind.select(vault).selectObject(v -> keychainManagerOptional.get().getPassphraseStoredProperty(v.getId())));
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
		vaultOptionsWindow.vault(vault.get()).build().showVaultOptionsWindow();
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
		if (keychainManagerOptional.isPresent() && vault.get() != null) {
			return keychainManagerOptional.get().getPassphraseStoredProperty(vault.get().getId()).get();
		} else return false;
	}
}

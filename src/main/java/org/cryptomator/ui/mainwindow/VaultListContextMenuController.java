package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.removevault.RemoveVaultComponent;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.EnumSet;
import java.util.Objects;

import static org.cryptomator.common.vaults.VaultState.Value.ERROR;
import static org.cryptomator.common.vaults.VaultState.Value.LOCKED;
import static org.cryptomator.common.vaults.VaultState.Value.MASTERKEY_MISSING;
import static org.cryptomator.common.vaults.VaultState.Value.MISSING;
import static org.cryptomator.common.vaults.VaultState.Value.NEEDS_MIGRATION;
import static org.cryptomator.common.vaults.VaultState.Value.UNLOCKED;
import static org.cryptomator.common.vaults.VaultState.Value.VAULT_CONFIG_MISSING;

@MainWindowScoped
public class VaultListContextMenuController implements FxController {

	private final ReadOnlyObjectProperty<Vault> selectedVault;
	private final Stage mainWindow;
	private final FxApplicationWindows appWindows;
	private final VaultService vaultService;
	private final KeychainManager keychain;
	private final RemoveVaultComponent.Builder removeVault;
	private final VaultOptionsComponent.Factory vaultOptionsWindow;
	private final ObservableValue<VaultState.Value> selectedVaultState;
	private final ObservableValue<Boolean> selectedVaultPassphraseStored;
	private final ObservableValue<Boolean> selectedVaultRemovable;
	private final ObservableValue<Boolean> selectedVaultUnlockable;
	private final ObservableValue<Boolean> selectedVaultLockable;

	@Inject
	VaultListContextMenuController(ObjectProperty<Vault> selectedVault, @MainWindow Stage mainWindow, FxApplicationWindows appWindows, VaultService vaultService, KeychainManager keychain, RemoveVaultComponent.Builder removeVault, VaultOptionsComponent.Factory vaultOptionsWindow) {
		this.selectedVault = selectedVault;
		this.mainWindow = mainWindow;
		this.appWindows = appWindows;
		this.vaultService = vaultService;
		this.keychain = keychain;
		this.removeVault = removeVault;
		this.vaultOptionsWindow = vaultOptionsWindow;

		this.selectedVaultState = selectedVault.flatMap(Vault::stateProperty).orElse(null);
		this.selectedVaultPassphraseStored = selectedVault.map(this::isPasswordStored).orElse(false);
		this.selectedVaultRemovable = selectedVaultState.map(EnumSet.of(LOCKED, MISSING, ERROR, NEEDS_MIGRATION, MASTERKEY_MISSING, VAULT_CONFIG_MISSING)::contains).orElse(false);
		this.selectedVaultUnlockable = selectedVaultState.map(LOCKED::equals).orElse(false);
		this.selectedVaultLockable = selectedVaultState.map(UNLOCKED::equals).orElse(false);
	}

	private boolean isPasswordStored(Vault vault) {
		return keychain.getPassphraseStoredProperty(vault.getId()).get();
	}

	@FXML
	public void didClickRemoveVault() {
		var vault = Objects.requireNonNull(selectedVault.get());
		removeVault.vault(vault).build().showRemoveVault();
	}

	@FXML
	public void didClickShowVaultOptions() {
		var vault = Objects.requireNonNull(selectedVault.get());
		vaultOptionsWindow.create(vault).showVaultOptionsWindow(SelectedVaultOptionsTab.ANY);
	}

	@FXML
	public void didClickUnlockVault() {
		var vault = Objects.requireNonNull(selectedVault.get());
		appWindows.startUnlockWorkflow(vault, mainWindow);
	}

	@FXML
	public void didClickLockVault() {
		var vault = Objects.requireNonNull(selectedVault.get());
		appWindows.startLockWorkflow(vault, mainWindow);
	}

	@FXML
	public void didClickRevealVault() {
		var vault = Objects.requireNonNull(selectedVault.get());
		vaultService.reveal(vault);
	}

	// Getter and Setter

	public ObservableValue<Boolean> selectedVaultUnlockableProperty() {
		return selectedVaultUnlockable;
	}

	public boolean isSelectedVaultUnlockable() {
		return selectedVaultUnlockable.getValue();
	}

	public ObservableValue<Boolean> selectedVaultLockableProperty() {
		return selectedVaultLockable;
	}

	public boolean isSelectedVaultLockable() {
		return selectedVaultLockable.getValue();
	}

	public ObservableValue<Boolean> selectedVaultRemovableProperty() {
		return selectedVaultRemovable;
	}

	public boolean isSelectedVaultRemovable() {
		return selectedVaultRemovable.getValue();
	}

	public ObservableValue<Boolean> selectedVaultPassphraseStoredProperty() {
		return selectedVaultPassphraseStored;
	}

	public boolean isSelectedVaultPassphraseStored() {
		return selectedVaultPassphraseStored.getValue();
	}
}

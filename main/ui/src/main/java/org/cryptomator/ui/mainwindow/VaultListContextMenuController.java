package org.cryptomator.ui.mainwindow;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import com.tobiasdiez.easybind.optional.OptionalBinding;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.removevault.RemoveVaultComponent;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.EnumSet;
import java.util.Optional;

import static org.cryptomator.common.vaults.VaultState.Value.*;

@MainWindowScoped
public class VaultListContextMenuController implements FxController {

	private final ObservableOptionalValue<Vault> selectedVault;
	private final Stage mainWindow;
	private final FxApplication application;
	private final KeychainManager keychain;
	private final RemoveVaultComponent.Builder removeVault;
	private final VaultOptionsComponent.Builder vaultOptionsWindow;
	private final OptionalBinding<VaultState.Value> selectedVaultState;
	private final Binding<Boolean> selectedVaultPassphraseStored;
	private final Binding<Boolean> selectedVaultRemovable;
	private final Binding<Boolean> selectedVaultUnlockable;
	private final Binding<Boolean> selectedVaultLockable;

	@Inject
	VaultListContextMenuController(ObjectProperty<Vault> selectedVault, @MainWindow Stage mainWindow, FxApplication application, KeychainManager keychain, RemoveVaultComponent.Builder removeVault, VaultOptionsComponent.Builder vaultOptionsWindow) {
		this.selectedVault = EasyBind.wrapNullable(selectedVault);
		this.mainWindow = mainWindow;
		this.application = application;
		this.keychain = keychain;
		this.removeVault = removeVault;
		this.vaultOptionsWindow = vaultOptionsWindow;

		this.selectedVaultState = this.selectedVault.mapObservable(Vault::stateProperty);
		this.selectedVaultPassphraseStored = this.selectedVault.map(this::isPasswordStored).orElse(false);
		this.selectedVaultRemovable = selectedVaultState.map(EnumSet.of(LOCKED, MISSING, ERROR, NEEDS_MIGRATION)::contains).orElse(false);
		this.selectedVaultUnlockable = selectedVaultState.map(LOCKED::equals).orElse(false);
		this.selectedVaultLockable = selectedVaultState.map(UNLOCKED::equals).orElse(false);
	}

	private boolean isPasswordStored(Vault vault) {
		return keychain.getPassphraseStoredProperty(vault.getId()).get();
	}

	@FXML
	public void didClickRemoveVault() {
		selectedVault.ifValuePresent(v -> {
			removeVault.vault(v).build().showRemoveVault();
		});
	}

	@FXML
	public void didClickShowVaultOptions() {
		selectedVault.ifValuePresent(v -> {
			vaultOptionsWindow.vault(v).build().showVaultOptionsWindow(SelectedVaultOptionsTab.ANY);
		});
	}

	@FXML
	public void didClickUnlockVault() {
		selectedVault.ifValuePresent(v -> {
			application.startUnlockWorkflow(v, Optional.of(mainWindow));
		});
	}

	@FXML
	public void didClickLockVault() {
		selectedVault.ifValuePresent(v -> {
			application.startLockWorkflow(v, Optional.of(mainWindow));
		});
	}

	@FXML
	public void didClickRevealVault() {
		selectedVault.ifValuePresent(v -> {
			application.getVaultService().reveal(v);
		});
	}

	// Getter and Setter

	public Binding<Boolean> selectedVaultUnlockableProperty() {
		return selectedVaultUnlockable;
	}

	public boolean isSelectedVaultUnlockable() {
		return selectedVaultUnlockable.getValue();
	}

	public Binding<Boolean> selectedVaultLockableProperty() {
		return selectedVaultLockable;
	}

	public boolean isSelectedVaultLockable() {
		return selectedVaultLockable.getValue();
	}

	public Binding<Boolean> selectedVaultRemovableProperty() {
		return selectedVaultRemovable;
	}

	public boolean isSelectedVaultRemovable() {
		return selectedVaultRemovable.getValue();
	}

	public Binding<Boolean> selectedVaultPassphraseStoredProperty() {
		return selectedVaultPassphraseStored;
	}

	public boolean isSelectedVaultPassphraseStored() {
		return selectedVaultPassphraseStored.getValue();
	}
}

package org.cryptomator.ui.mainwindow;

import com.tobiasdiez.easybind.EasyBind;
import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.removevault.RemoveVaultComponent;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.Arrays;
import java.util.Optional;

import static org.cryptomator.common.vaults.VaultState.ERROR;
import static org.cryptomator.common.vaults.VaultState.LOCKED;
import static org.cryptomator.common.vaults.VaultState.MISSING;
import static org.cryptomator.common.vaults.VaultState.NEEDS_MIGRATION;
import static org.cryptomator.common.vaults.VaultState.UNLOCKED;

@MainWindowScoped
public class VaultListContextMenuController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultListContextMenuController.class);

	private final ReadOnlyObjectProperty<Vault> selectedVault;
	private final Stage mainWindow;
	private final FxApplication application;
	private final KeychainManager keychain;
	private final RemoveVaultComponent.Builder removeVault;
	private final VaultOptionsComponent.Builder vaultOptionsWindow;
	private final Binding<VaultState> selectedVaultState;
	private final BooleanBinding selectedVaultPassphraseStored;
	private final BooleanBinding selectedVaultRemovable;
	private final BooleanBinding selectedVaultUnlockable;
	private final BooleanBinding selectedVaultLockable;

	@Inject
	VaultListContextMenuController(ObjectProperty<Vault> selectedVault, @MainWindow Stage mainWindow, FxApplication application, KeychainManager keychain, RemoveVaultComponent.Builder removeVault, VaultOptionsComponent.Builder vaultOptionsWindow) {
		this.selectedVault = selectedVault;
		this.mainWindow = mainWindow;
		this.application = application;
		this.keychain = keychain;
		this.removeVault = removeVault;
		this.vaultOptionsWindow = vaultOptionsWindow;

		this.selectedVaultState = EasyBind.wrapNullable(selectedVault).mapObservable(Vault::stateProperty).orElse((VaultState) null);
		this.selectedVaultPassphraseStored = Bindings.createBooleanBinding(this::isSelectedVaultPassphraseStored, selectedVault);
		this.selectedVaultRemovable = Bindings.createBooleanBinding(() -> selectedVaultIsInState(LOCKED, MISSING, ERROR, NEEDS_MIGRATION), selectedVaultState);
		this.selectedVaultUnlockable = Bindings.createBooleanBinding(() -> selectedVaultIsInState(LOCKED), selectedVaultState);
		this.selectedVaultLockable = Bindings.createBooleanBinding(() -> selectedVaultIsInState(UNLOCKED), selectedVaultState);
	}

	private boolean selectedVaultIsInState(VaultState... states) {
		var state = selectedVaultState.getValue();
		return Arrays.stream(states).anyMatch(s -> state == s);
	}

	@FXML
	public void didClickRemoveVault() {
		Vault v = selectedVault.get();
		if (v != null) {
			removeVault.vault(v).build().showRemoveVault();
		} else {
			LOG.debug("Cannot remove a vault if none is selected.");
		}
	}

	@FXML
	public void didClickShowVaultOptions() {
		Vault v = selectedVault.get();
		if (v != null) {
			vaultOptionsWindow.vault(v).build().showVaultOptionsWindow(SelectedVaultOptionsTab.ANY);
		} else {
			LOG.debug("Cannot open vault options if none is selected.");
		}
	}

	@FXML
	public void didClickUnlockVault() {
		Vault v = selectedVault.get();
		if (v != null) {
			application.startUnlockWorkflow(v, Optional.of(mainWindow));
		} else {
			LOG.debug("Cannot unlock vault if none is selected.");
		}
	}

	@FXML
	public void didClickLockVault() {
		Vault v = selectedVault.get();
		if (v != null) {
			application.startLockWorkflow(v, Optional.of(mainWindow));
		} else {
			LOG.debug("Cannot lock vault if none is selected.");
		}
	}

	@FXML
	public void didClickRevealVault() {
		Vault v = selectedVault.get();
		if (v != null) {
			application.getVaultService().reveal(v);
		} else {
			LOG.debug("Cannot reveal vault if none is selected.");
		}
	}

	// Getter and Setter

	public BooleanBinding selectedVaultUnlockableProperty() {
		return selectedVaultUnlockable;
	}

	public boolean isSelectedVaultUnlockable() {
		return selectedVaultUnlockable.get();
	}

	public BooleanBinding selectedVaultLockableProperty() {
		return selectedVaultLockable;
	}

	public boolean isSelectedVaultLockable() {
		return selectedVaultLockable.get();
	}

	public BooleanBinding selectedVaultRemovableProperty() {
		return selectedVaultRemovable;
	}

	public boolean isSelectedVaultRemovable() {
		return selectedVaultRemovable.get();
	}

	public BooleanBinding selectedVaultPassphraseStoredProperty() {
		return selectedVaultPassphraseStored;
	}

	public boolean isSelectedVaultPassphraseStored() {
		if (selectedVault.get() == null) {
			return false;
		} else {
			return keychain.getPassphraseStoredProperty(selectedVault.get().getId()).get();
		}
	}
}

package org.cryptomator.ui.mainwindow;

import org.cryptomator.common.keychain.KeychainManager;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.addvaultwizard.AddVaultWizardComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.removevault.RemoveVaultComponent;
import org.cryptomator.ui.vaultoptions.SelectedVaultOptionsTab;
import org.cryptomator.ui.vaultoptions.VaultOptionsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import java.util.Arrays;
import java.util.Optional;

import static org.cryptomator.common.vaults.VaultState.*;

@MainWindowScoped
public class VaultListController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultListController.class);

	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> selectedVault;
	private final ObjectProperty<VaultState> selectedVaultState;
	private final BooleanProperty selectedVaultPassphraseStored;
	private final VaultListCellFactory cellFactory;
	private final Stage mainWindow;
	private final FxApplication application;
	private final KeychainManager keychain;
	private final AddVaultWizardComponent.Builder addVaultWizard;
	private final RemoveVaultComponent.Builder removeVault;
	private final VaultOptionsComponent.Builder vaultOptionsWindow;
	private final BooleanBinding emptyVaultList;
	private final BooleanBinding selectedVaultRemovable;
	private final BooleanBinding selectedVaultUnlockable;
	private final BooleanBinding selectedVaultLockable;

	public ListView<Vault> vaultList;

	@Inject
	VaultListController(ObservableList<Vault> vaults, ObjectProperty<Vault> selectedVault, VaultListCellFactory cellFactory, @MainWindow Stage mainWindow, FxApplication application, KeychainManager keychain, AddVaultWizardComponent.Builder addVaultWizard, RemoveVaultComponent.Builder removeVault, VaultOptionsComponent.Builder vaultOptionsWindow) {
		this.vaults = vaults;
		this.selectedVault = selectedVault;
		this.cellFactory = cellFactory;
		this.mainWindow = mainWindow;
		this.application = application;
		this.keychain = keychain;
		this.addVaultWizard = addVaultWizard;
		this.removeVault = removeVault;
		this.emptyVaultList = Bindings.isEmpty(vaults);
		this.vaultOptionsWindow = vaultOptionsWindow;
		this.selectedVaultState = new SimpleObjectProperty<>(null);
		this.selectedVaultPassphraseStored = new SimpleBooleanProperty(false);
		this.selectedVaultRemovable = Bindings.createBooleanBinding(() -> selectedVaultIsInState(LOCKED, MISSING, ERROR, NEEDS_MIGRATION), selectedVaultState);
		this.selectedVaultUnlockable = Bindings.createBooleanBinding(() -> selectedVaultIsInState(LOCKED), selectedVaultState);
		this.selectedVaultLockable = Bindings.createBooleanBinding(() -> selectedVaultIsInState(UNLOCKED), selectedVaultState);
		selectedVault.addListener(this::selectedVaultDidChange);
	}

	public void initialize() {
		vaultList.setItems(vaults);
		vaultList.setCellFactory(cellFactory);
		selectedVault.bind(vaultList.getSelectionModel().selectedItemProperty());
		if (selectedVault.get() != null) {
			selectedVaultState.set(selectedVault.get().getState());
			selectedVaultPassphraseStored.bind(keychain.getPassphraseStoredProperty(selectedVault.get().getId()));
		}
		vaults.addListener((ListChangeListener.Change<? extends Vault> c) -> {
			while (c.next()) {
				if (c.wasAdded()) {
					Vault anyAddedVault = c.getAddedSubList().get(0);
					vaultList.getSelectionModel().select(anyAddedVault);
				}
			}
		});
	}

	private void selectedVaultDidChange(@SuppressWarnings("unused") ObservableValue<? extends Vault> observableValue, @SuppressWarnings("unused") Vault oldValue, Vault newValue) {
		if (oldValue != null) {
			selectedVaultState.unbind();
			selectedVaultPassphraseStored.unbind();
		}
		if (newValue == null) {
			return;
		}
		VaultListManager.redetermineVaultState(newValue);
		selectedVaultState.bind(newValue.stateProperty());
		selectedVaultPassphraseStored.bind(keychain.getPassphraseStoredProperty(newValue.getId()));
	}

	private boolean selectedVaultIsInState(VaultState... states) {
		var state = selectedVaultState.get();
		return Arrays.stream(states).anyMatch(s -> state == s);
	}


	@FXML
	public void didClickAddVault() {
		addVaultWizard.build().showAddVaultWizard();
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

	public BooleanBinding emptyVaultListProperty() {
		return emptyVaultList;
	}

	public boolean isEmptyVaultList() {
		return emptyVaultList.get();
	}

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

	public BooleanProperty selectedVaultPassphraseStoredProperty() {
		return selectedVaultPassphraseStored;
	}

	public boolean isSelectedVaultPassphraseStored() {
		return selectedVaultPassphraseStored.get();
	}
}

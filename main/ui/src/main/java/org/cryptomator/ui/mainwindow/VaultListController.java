package org.cryptomator.ui.mainwindow;

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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import java.util.Optional;

@MainWindowScoped
public class VaultListController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultListController.class);

	private final ObservableList<Vault> vaults;
	private final ObjectProperty<Vault> selectedVault;
	private final BooleanProperty selectedVaultLocked;
	private final BooleanProperty selectedVaultUnlocked;
	private final VaultListCellFactory cellFactory;
	private final Stage mainWindow;
	private final FxApplication application;
	private final AddVaultWizardComponent.Builder addVaultWizard;
	private final RemoveVaultComponent.Builder removeVault;
	private final VaultOptionsComponent.Builder vaultOptionsWindow;
	private final BooleanBinding noVaultSelected;
	private final BooleanBinding emptyVaultList;

	public ListView<Vault> vaultList;

	@Inject
	VaultListController(ObservableList<Vault> vaults, ObjectProperty<Vault> selectedVault, VaultListCellFactory cellFactory, @MainWindow Stage mainWindow, FxApplication application, AddVaultWizardComponent.Builder addVaultWizard, RemoveVaultComponent.Builder removeVault, VaultOptionsComponent.Builder vaultOptionsWindow) {
		this.vaults = vaults;
		this.selectedVault = selectedVault;
		this.cellFactory = cellFactory;
		this.mainWindow = mainWindow;
		this.application = application;
		this.addVaultWizard = addVaultWizard;
		this.removeVault = removeVault;
		this.noVaultSelected = selectedVault.isNull();
		this.emptyVaultList = Bindings.isEmpty(vaults);
		this.vaultOptionsWindow = vaultOptionsWindow;
		this.selectedVaultLocked = new SimpleBooleanProperty(false);
		this.selectedVaultUnlocked = new SimpleBooleanProperty(false);
		selectedVault.addListener(this::selectedVaultDidChange);
	}

	public void initialize() {
		vaultList.setItems(vaults);
		vaultList.setCellFactory(cellFactory);
		selectedVault.bind(vaultList.getSelectionModel().selectedItemProperty());
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
			oldValue.stateProperty().removeListener((ChangeListener<? super VaultState>) this::updateVaultStateDependencies);
		}
		if (newValue == null) {
			return;
		}
		VaultListManager.redetermineVaultState(newValue);
		setVaultStateDependencies(newValue.getState());
		newValue.stateProperty().addListener((ChangeListener<? super VaultState>) this::updateVaultStateDependencies);
	}

	private void setVaultStateDependencies(VaultState state) {
		selectedVaultLocked.setValue(state == VaultState.LOCKED);
		selectedVaultUnlocked.setValue(state == VaultState.UNLOCKED);
	}

	private void updateVaultStateDependencies(ObservableValue<? extends VaultState> observableValue, VaultState oldVal, VaultState newVal) {
		selectedVaultLocked.setValue(newVal == VaultState.LOCKED);
		selectedVaultUnlocked.setValue(newVal == VaultState.UNLOCKED);
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

	// Getter and Setter

	public BooleanBinding emptyVaultListProperty() {
		return emptyVaultList;
	}

	public boolean isEmptyVaultList() {
		return emptyVaultList.get();
	}

	public BooleanBinding noVaultSelectedProperty() {
		return noVaultSelected;
	}

	public boolean isNoVaultSelected() {
		return noVaultSelected.get();
	}

	public BooleanProperty selectedVaultLockedProperty() {
		return selectedVaultLocked;
	}

	public boolean isSelectedVaultLocked() {
		return selectedVaultLocked.get();
	}

	public BooleanProperty selectedVaultUnlockedProperty() {
		return selectedVaultUnlocked;
	}

	public boolean isSelectedVaultUnlocked() {
		return selectedVaultUnlocked.get();
	}
}

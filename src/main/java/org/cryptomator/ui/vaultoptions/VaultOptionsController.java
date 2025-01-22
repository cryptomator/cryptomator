package org.cryptomator.ui.vaultoptions;

import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultState;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.hub.HubKeyLoadingStrategy;
import org.cryptomator.ui.keyloading.masterkeyfile.MasterkeyFileLoadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@VaultOptionsScoped
public class VaultOptionsController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(VaultOptionsController.class);

	private final Stage window;
	private final Vault vault;
	private final ObjectProperty<SelectedVaultOptionsTab> selectedTabProperty;
	public TabPane tabPane;
	public Tab generalTab;
	public Tab mountTab;
	public Tab keyTab;
	public Tab hubTab;

	@Inject
	VaultOptionsController(@VaultOptionsWindow Stage window, @VaultOptionsWindow Vault vault, ObjectProperty<SelectedVaultOptionsTab> selectedTabProperty) {
		this.window = window;
		this.vault = vault;
		this.selectedTabProperty = selectedTabProperty;
	}

	@FXML
	public void initialize() {
		window.setOnShowing(this::windowWillAppear);
		selectedTabProperty.addListener(observable -> this.selectChosenTab());
		tabPane.getSelectionModel().selectedItemProperty().addListener(observable -> this.selectedTabChanged());
		var vaultKeyLoader = vault.getVaultSettings().keyLoader.get();
		if(!vaultKeyLoader.equals(MasterkeyFileLoadingStrategy.SCHEME)){
			tabPane.getTabs().remove(keyTab);
		}
		if(!(vaultKeyLoader.equals(HubKeyLoadingStrategy.SCHEME_HUB_HTTP) || vaultKeyLoader.equals(HubKeyLoadingStrategy.SCHEME_HUB_HTTPS))){
			tabPane.getTabs().remove(hubTab);
		}

		vault.stateProperty().addListener(observable -> {
			tabPane.setDisable(vault.getState().equals(VaultState.Value.UNLOCKED));
		});
	}

	private void selectChosenTab() {
		Tab toBeSelected = getTabToSelect(selectedTabProperty.get());
		tabPane.getSelectionModel().select(toBeSelected);
	}

	private Tab getTabToSelect(SelectedVaultOptionsTab selectedTab) {
		return switch (selectedTab) {
			case ANY, GENERAL -> generalTab;
			case MOUNT -> mountTab;
			case KEY -> keyTab;
			case HUB -> hubTab;
		};
	}

	private void selectedTabChanged() {
		Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
		try {
			SelectedVaultOptionsTab selectedVaultOptionsTab = SelectedVaultOptionsTab.valueOf(selectedTab.getId());
			selectedTabProperty.set(selectedVaultOptionsTab);
		} catch (IllegalArgumentException e) {
			LOG.error("Unknown vault options tab id: {}", selectedTab.getId());
		}
	}

	private void windowWillAppear(@SuppressWarnings("unused") WindowEvent windowEvent) {
		selectChosenTab();
	}

}

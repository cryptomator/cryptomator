package org.cryptomator.ui.vaultoptions;

import org.cryptomator.ui.common.FxController;
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
	private final ObjectProperty<SelectedVaultOptionsTab> selectedTabProperty;
	public TabPane tabPane;
	public Tab generalTab;
	public Tab mountTab;
	public Tab keyTab;

	@Inject
	VaultOptionsController(@VaultOptionsWindow Stage window, ObjectProperty<SelectedVaultOptionsTab> selectedTabProperty) {
		this.window = window;
		this.selectedTabProperty = selectedTabProperty;
	}

	@FXML
	public void initialize() {
		window.setOnShowing(this::windowWillAppear);
		selectedTabProperty.addListener(observable -> this.selectChosenTab());
		tabPane.getSelectionModel().selectedItemProperty().addListener(observable -> this.selectedTabChanged());
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

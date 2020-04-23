package org.cryptomator.ui.preferences;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@PreferencesScoped
public class PreferencesController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(PreferencesController.class);

	private final Stage window;
	private final ObjectProperty<SelectedPreferencesTab> selectedTabProperty;
	private final BooleanBinding updateAvailable;
	public TabPane tabPane;
	public Tab generalTab;
	public Tab volumeTab;
	public Tab updatesTab;
	public Tab donationKeyTab;
	public Tab aboutTab;

	@Inject
	public PreferencesController(@PreferencesWindow Stage window, ObjectProperty<SelectedPreferencesTab> selectedTabProperty, UpdateChecker updateChecker) {
		this.window = window;
		this.selectedTabProperty = selectedTabProperty;
		this.updateAvailable = updateChecker.latestVersionProperty().isNotNull();
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

	private Tab getTabToSelect(SelectedPreferencesTab selectedTab) {
		return switch (selectedTab) {
			case UPDATES -> updatesTab;
			case VOLUME -> volumeTab;
			case DONATION_KEY -> donationKeyTab;
			case GENERAL -> generalTab;
			case ABOUT -> aboutTab;
			case ANY -> updateAvailable.get() ? updatesTab : generalTab;
		};
	}

	private void selectedTabChanged() {
		Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
		try {
			SelectedPreferencesTab selectedPreferencesTab = SelectedPreferencesTab.valueOf(selectedTab.getId());
			selectedTabProperty.set(selectedPreferencesTab);
		} catch (IllegalArgumentException e) {
			LOG.error("Unknown preferences tab id: {}", selectedTab.getId());
		}
	}

	private void windowWillAppear(@SuppressWarnings("unused") WindowEvent windowEvent) {
		selectChosenTab();
	}

}

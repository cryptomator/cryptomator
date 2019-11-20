package org.cryptomator.ui.preferences;

import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.UpdateChecker;

import javax.inject.Inject;

@PreferencesScoped
public class PreferencesController implements FxController {

	private final Stage window;
	private final ObjectProperty<SelectedPreferencesTab> selectedTabProperty;
	private final BooleanBinding updateAvailable;
	public TabPane tabPane;
	public Tab generalTab;
	public Tab volumeTab;
	public Tab updatesTab;
	public Tab donationKeyTab;

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
	}

	private void selectChosenTab() {
		Tab toBeSelected = getTabToSelect(selectedTabProperty.get());
		tabPane.getSelectionModel().select(toBeSelected);
	}

	private Tab getTabToSelect(SelectedPreferencesTab selectedTab) {
		switch (selectedTab) {
			case UPDATES:
				return updatesTab;
			case VOLUME:
				return volumeTab;
			case DONATION_KEY:
				return donationKeyTab;
			case GENERAL:
				return generalTab;
			case ANY:
			default:
				return updateAvailable.get() ? updatesTab : generalTab;
		}
	}

	private void windowWillAppear(@SuppressWarnings("unused") WindowEvent windowEvent) {
		selectChosenTab();
	}

}

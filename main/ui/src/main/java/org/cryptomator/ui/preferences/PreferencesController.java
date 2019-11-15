package org.cryptomator.ui.preferences;

import javafx.beans.binding.BooleanBinding;
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
	private final BooleanBinding updateAvailable;
	public TabPane tabPane;
	public Tab updatesTab;

	@Inject
	public PreferencesController(@PreferencesWindow Stage window, UpdateChecker updateChecker) {
		this.window = window;
		this.updateAvailable = updateChecker.latestVersionProperty().isNotNull();
	}

	@FXML
	public void initialize() {
		window.setOnShowing(this::windowWillAppear);
	}

	private void windowWillAppear(@SuppressWarnings("unused") WindowEvent windowEvent) {
		if (updateAvailable.get()) {
			tabPane.getSelectionModel().select(updatesTab);
		}
	}

}

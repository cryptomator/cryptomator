package org.cryptomator.ui.preferences;

import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
	public Tab generalTab;
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

	private void windowWillAppear(WindowEvent windowEvent) {
		if (updateAvailable.get()) {
			tabPane.getSelectionModel().select(updatesTab);
		}

		KeyCombination cmdW = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
		window.getScene().getAccelerators().put(cmdW, window::close);
	}

}

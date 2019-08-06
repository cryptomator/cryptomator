package org.cryptomator.ui.vaultoptions;

import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@VaultOptionsScoped
public class VaultOptionsController implements FxController {

	private final Stage window;

	@Inject
	VaultOptionsController(@VaultOptionsWindow Stage window) {
		this.window = window;
	}

	@FXML
	public void initialize() {
		window.setOnShowing(this::windowWillAppear);
	}

	private void windowWillAppear(WindowEvent windowEvent) {
		KeyCombination cmdW = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
		window.getScene().getAccelerators().put(cmdW, window::close);
	}

}

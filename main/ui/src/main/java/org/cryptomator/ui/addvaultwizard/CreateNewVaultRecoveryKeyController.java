package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CreateNewVaultRecoveryKeyController implements FxController {

	private final Stage window;
	private final Lazy<Scene> successScene;

	@Inject
	CreateNewVaultRecoveryKeyController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene) {
		this.window = window;
		this.successScene = successScene;
	}

	@FXML
	public void next() {
		window.setScene(successScene.get());
	}
}

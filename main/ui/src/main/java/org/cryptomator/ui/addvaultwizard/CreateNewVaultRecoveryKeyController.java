package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;
import javax.inject.Named;

public class CreateNewVaultRecoveryKeyController implements FxController {

	private final Stage window;
	private final Lazy<Scene> successScene;
	private final StringProperty recoveryKeyProperty;

	@Inject
	CreateNewVaultRecoveryKeyController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, @Named("recoveryKey")StringProperty recoveryKey) {
		this.window = window;
		this.successScene = successScene;
		this.recoveryKeyProperty = recoveryKey;
	}

	@FXML
	public void next() {
		window.setScene(successScene.get());
	}

	/* Getter/Setter */

	public String getRecoveryKey() {
		return recoveryKeyProperty.get();
	}

	public StringProperty recoveryKeyProperty() {
		return recoveryKeyProperty;
	}
}

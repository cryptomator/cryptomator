package org.cryptomator.ui.addvaultwizard;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javax.inject.Named;

@AddVaultWizardScoped
public class AddVaultGenericErrorController implements FxController {

	private final Stage window;
	private final ReadOnlyObjectProperty<Scene> previousScene;
	private final BooleanBinding returnToPreviousSceneAllowed;

	@Inject
	AddVaultGenericErrorController(@AddVaultWizardWindow Stage window, @Named("genericErrorReturnScene") ObjectProperty<Scene> previousScene) {
		this.window = window;
		this.previousScene = previousScene;
		this.returnToPreviousSceneAllowed = previousScene.isNotNull();
	}

	@FXML
	public void back() {
		assert previousScene.get() != null; // otherwise button should be disabled
		window.setScene(previousScene.get());
	}

	@FXML
	public void close(){
		window.close();
	}

	/* Getter/Setter */

	public BooleanBinding returnToPreviousSceneAllowedProperty() {
		return returnToPreviousSceneAllowed;
	}

	public boolean isReturnToPreviousSceneAllowed() {
		return returnToPreviousSceneAllowed.get();
	}
}

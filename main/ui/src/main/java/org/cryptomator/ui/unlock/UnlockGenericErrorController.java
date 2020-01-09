package org.cryptomator.ui.unlock;

import dagger.Lazy;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Inject;

@UnlockScoped
public class UnlockGenericErrorController implements FxController {

	private final Stage window;
	private final Lazy<Scene> unlockScene;

	@Inject
	UnlockGenericErrorController(@UnlockWindow Stage window, @FxmlScene(FxmlFile.UNLOCK) Lazy<Scene> unlockScene) {
		this.window = window;
		this.unlockScene = unlockScene;
	}

	@FXML
	public void back() {
		window.setScene(unlockScene.get());
	}
}

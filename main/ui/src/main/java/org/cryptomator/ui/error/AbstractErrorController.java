package org.cryptomator.ui.error;

import org.cryptomator.ui.common.FxController;

import javax.annotation.Nullable;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AbstractErrorController implements FxController {

	protected final Stage window;
	protected final Scene returnScene;

	public AbstractErrorController(@ErrorReport Stage window, @ErrorReport @Nullable Scene returnScene) {
		this.returnScene = returnScene;
		this.window = window;
	}

	@FXML
	public void back() {
		if (this.returnScene != null) {
			this.window.setScene(this.returnScene);
		}
	}

	@FXML
	public void close() {
		this.window.close();
	}

	public boolean isReturnScenePresent() {
		return this.returnScene != null;
	}
}
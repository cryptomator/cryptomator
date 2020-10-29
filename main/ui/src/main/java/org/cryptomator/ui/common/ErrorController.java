package org.cryptomator.ui.common;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ErrorController implements FxController {

	private final String stackTrace;
	private final Scene previousScene;
	private final Stage window;

	@Inject
	ErrorController(@Named("stackTrace") String stackTrace, @Nullable Scene previousScene, Stage window) {
		this.stackTrace = stackTrace;
		this.previousScene = previousScene;
		this.window = window;
	}

	@FXML
	public void back() {
		if (previousScene != null) {
			window.setScene(previousScene);
		}
	}

	@FXML
	public void close() {
		window.close();
	}

	/* Getter/Setter */

	public boolean isPreviousScenePresent() {
		return previousScene != null;
	}

	public String getStackTrace() {
		return stackTrace;
	}
}

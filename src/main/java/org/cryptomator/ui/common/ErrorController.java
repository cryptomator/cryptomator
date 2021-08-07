package org.cryptomator.ui.common;

import org.cryptomator.common.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ErrorController implements FxController {

	private static final String SEARCH_URL_FORMAT = "https://github.com/cryptomator/cryptomator/issues?q=%s";

	private final Application application;
	private final String stackTrace;
	private final String errorCode;
	private final Scene previousScene;
	private final Stage window;

	@Inject
	ErrorController(Application application, @Named("stackTrace") String stackTrace, @Named("errorCode") String errorCode, @Nullable Scene previousScene, Stage window) {
		this.application = application;
		this.stackTrace = stackTrace;
		this.errorCode = errorCode;
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

	@FXML
	public void searchErrorCode() {
		application.getHostServices().showDocument(SEARCH_URL_FORMAT.formatted(URLEncoder.encode(getErrorCode(), StandardCharsets.UTF_8)));
	}

	/* Getter/Setter */

	public boolean isPreviousScenePresent() {
		return previousScene != null;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	public String getErrorCode() {
		return errorCode;
	}
}

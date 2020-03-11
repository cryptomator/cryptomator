package org.cryptomator.ui.wrongfilealert;

import javafx.application.Application;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@WrongFileAlertScoped
public class WrongFileAlertController implements FxController {

	private static final String DOCUMENTATION_URI = "https://docs.cryptomator.org";

	private Application app;
	private final Stage window;

	@Inject
	public WrongFileAlertController(@WrongFileAlertWindow Stage window, Application app) {
		this.window = window;
		this.app = app;
	}

	public void close() {
		window.close();
	}

	@FXML
	public void openDocumentation() {
		app.getHostServices().showDocument(DOCUMENTATION_URI);
	}
}

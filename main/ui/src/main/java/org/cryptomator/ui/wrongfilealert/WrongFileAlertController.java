package org.cryptomator.ui.wrongfilealert;

import javafx.stage.Stage;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@WrongFileAlertScoped
public class WrongFileAlertController implements FxController {

	private final Stage window;

	@Inject
	public WrongFileAlertController(@WrongFileAlertWindow Stage window) {
		this.window = window;
	}

	public void close() {
		window.close();
	}
}

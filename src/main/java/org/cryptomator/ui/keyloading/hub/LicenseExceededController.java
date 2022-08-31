package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.KeyLoading;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

public class LicenseExceededController implements FxController {

	private final Stage window;

	@Inject
	public LicenseExceededController(@KeyLoading Stage window) {
		this.window = window;
	}

	@FXML
	public void close() {
		window.close();
	}
}

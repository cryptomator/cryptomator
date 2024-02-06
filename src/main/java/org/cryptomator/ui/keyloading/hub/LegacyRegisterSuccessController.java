package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;

@KeyLoadingScoped
public class LegacyRegisterSuccessController implements FxController {
	private final Stage window;

	@Inject
	public LegacyRegisterSuccessController(@KeyLoading Stage window) {
		this.window = window;
	}

	@FXML
	public void close() {
		window.close();
	}
}

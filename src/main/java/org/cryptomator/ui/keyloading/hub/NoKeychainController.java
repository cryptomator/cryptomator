package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.KeyLoading;

import javax.inject.Inject;
import javafx.stage.Stage;

public class NoKeychainController implements FxController {

	private final Stage window;

	@Inject
	public NoKeychainController(@KeyLoading Stage window) {
		this.window = window;
	}


	public void cancel() {
		window.close();
	}

}

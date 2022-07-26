package org.cryptomator.ui.keyloading.hub;

import com.nimbusds.jose.JWEObject;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.KeyLoading;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import java.util.concurrent.CompletableFuture;

public class RegisterFailedController implements FxController {

	private final Stage window;
	private final CompletableFuture<JWEObject> result;

	@Inject
	public RegisterFailedController(@KeyLoading Stage window, CompletableFuture<JWEObject> result) {
		this.window = window;
		this.result = result;
	}

	@FXML
	public void close() {
		window.close();
	}


}

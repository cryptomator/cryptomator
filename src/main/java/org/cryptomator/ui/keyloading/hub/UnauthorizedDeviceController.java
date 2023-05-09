package org.cryptomator.ui.keyloading.hub;

import com.nimbusds.jose.JWEObject;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.util.concurrent.CompletableFuture;

@KeyLoadingScoped
public class UnauthorizedDeviceController implements FxController {

	private final Stage window;
	private final CompletableFuture<ReceivedKey> result;

	@Inject
	public UnauthorizedDeviceController(@KeyLoading Stage window, CompletableFuture<ReceivedKey> result) {
		this.window = window;
		this.result = result;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void close() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		result.cancel(true);
	}
}

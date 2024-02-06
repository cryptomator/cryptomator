package org.cryptomator.ui.keyloading.hub;

import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoading;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.util.concurrent.CompletableFuture;

public class RegisterSuccessController implements FxController {

	private final Stage window;
	private final CompletableFuture<ReceivedKey> result;
	private final Lazy<Scene> receiveKeyScene;
	private final ReceiveKeyController receiveKeyController;

	@Inject
	public RegisterSuccessController(@KeyLoading Stage window, CompletableFuture<ReceivedKey> result, @FxmlScene(FxmlFile.HUB_RECEIVE_KEY) Lazy<Scene> receiveKeyScene, ReceiveKeyController receiveKeyController) {
		this.window = window;
		this.result = result;
		this.receiveKeyScene = receiveKeyScene;
		this.receiveKeyController = receiveKeyController;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void complete() {
		window.setScene(receiveKeyScene.get());
		receiveKeyController.receiveKey();
	}

	private void windowClosed(WindowEvent windowEvent) {
		result.cancel(true);
	}


}

package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.util.concurrent.CompletableFuture;

@KeyLoadingScoped
public class RequireAccountInitController implements FxController {

	private final Application application;
	private final HubConfig hubConfig;
	private final Stage window;
	private final CompletableFuture<ReceivedKey> result;

	@Inject
	public RequireAccountInitController(Application application, HubConfig hubConfig, @KeyLoading Stage window, CompletableFuture<ReceivedKey> result) {
		this.application = application;
		this.hubConfig = hubConfig;
		this.window = window;
		this.result = result;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void completeSetup() {
		application.getHostServices().showDocument(hubConfig.getWebappBaseUrl().resolve("profile").toString());
		close();
	}

	@FXML
	public void close() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		result.cancel(true);
	}
}

package org.cryptomator.ui.keyloading.hub;

import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@KeyLoadingScoped
public class UnauthorizedDeviceController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UnauthorizedDeviceController.class);

	private final Stage window;
	private final UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result;

	@Inject
	public UnauthorizedDeviceController(@KeyLoading Stage window, UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result) {
		this.window = window;
		this.result = result;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void close() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		// if not already interacted, mark this workflow as cancelled:
		if (result.awaitingInteraction().get()) {
			LOG.debug("Authorization cancelled. Device not authorized.");
			result.interacted(HubKeyLoadingModule.HubLoadingResult.CANCELLED);
		}
	}
}

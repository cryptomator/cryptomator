package org.cryptomator.ui.keyloading.hub;

import com.google.common.io.BaseEncoding;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.security.KeyPair;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class RegisterDeviceController implements FxController {

	private final Application application;
	private final Stage window;
	private final HubConfig hubConfig;
	private final KeyPair keyPair;
	private final UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result;

	@Inject
	public RegisterDeviceController(Application application, @KeyLoading Stage window, HubConfig hubConfig, AtomicReference<KeyPair> keyPairRef, UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result) {
		this.application = application;
		this.window = window;
		this.hubConfig = hubConfig;
		this.keyPair = Objects.requireNonNull(keyPairRef.get());
		this.result = result;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void browse() {
		var deviceKey = BaseEncoding.base64Url().omitPadding().encode(keyPair.getPublic().getEncoded());
		var url = hubConfig.deviceRegistrationUrl + "?device_key=" + deviceKey;
		// TODO append further params (including hmac of shown verification code)
		application.getHostServices().showDocument(url);
	}

	@FXML
	public void close() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		// if not already interacted, mark this workflow as cancelled:
		if (result.awaitingInteraction().get()) {
			result.interacted(HubKeyLoadingModule.HubLoadingResult.CANCELLED);
		}
	}

}

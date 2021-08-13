package org.cryptomator.ui.keyloading.hub;

import com.google.common.io.BaseEncoding;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class RegisterDeviceController implements FxController {

	private final Application application;
	private final Stage window;
	private final HubConfig hubConfig;
	private final KeyPair keyPair;
	private final UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result;
	private final String verificationCode;

	@Inject
	public RegisterDeviceController(Application application, SecureRandom csprng, @KeyLoading Stage window, HubConfig hubConfig, AtomicReference<KeyPair> keyPairRef, UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result) {
		this.application = application;
		this.window = window;
		this.hubConfig = hubConfig;
		this.keyPair = Objects.requireNonNull(keyPairRef.get());
		this.result = result;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.verificationCode = String.format("%06d", csprng.nextInt(1_000_000));
	}

	@FXML
	public void browse() {
		var deviceKey = BaseEncoding.base64Url().omitPadding().encode(keyPair.getPublic().getEncoded());
		var deviceId = "desktop-app"; // TODO use actual device id
		var hash = computeVerificationHash(deviceId + deviceKey + verificationCode);
		var url = hubConfig.deviceRegistrationUrl + "?device_key=" + deviceKey + "&device_id=" + deviceId + "&verification_hash=" + hash;
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

	private static String computeVerificationHash(String input) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			digest.update(StandardCharsets.UTF_8.encode(input));
			return BaseEncoding.base64Url().omitPadding().encode(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every implementation of the Java platform is required to support SHA-256.");
		}
	}

	/* Getter */

	public String getVerificationCode() {
		return verificationCode;
	}

}

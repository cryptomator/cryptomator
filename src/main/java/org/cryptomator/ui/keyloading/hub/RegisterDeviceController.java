package org.cryptomator.ui.keyloading.hub;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.cryptomator.common.settings.DeviceKey;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class RegisterDeviceController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RegisterDeviceController.class);
	private static final Gson GSON = new GsonBuilder().setLenient().create();

	private final Stage window;
	private final HubConfig hubConfig;
	private final String bearerToken;
	private final String deviceId;
	private final P384KeyPair keyPair;
	private final UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result;
	private final DecodedJWT jwt;
	private final HttpClient httpClient;

	public TextField deviceNameField;

	@Inject
	public RegisterDeviceController(@KeyLoading Stage window, ExecutorService executor, HubConfig hubConfig, @Named("deviceId") String deviceId, DeviceKey deviceKey, UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result, @Named("bearerToken") AtomicReference<String> bearerToken) {
		this.window = window;
		this.hubConfig = hubConfig;
		this.deviceId = deviceId;
		this.keyPair = Objects.requireNonNull(deviceKey.get());
		this.result = result;
		this.bearerToken = Objects.requireNonNull(bearerToken.get());
		this.jwt = JWT.decode(this.bearerToken);
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.httpClient = HttpClient.newBuilder().executor(executor).build();
	}

	@FXML
	public void register() {
		var keyUri = URI.create("http://localhost:9090/devices/" + deviceId); // TODO lol hubConfig.deviceRegistrationUrl
		var deviceKey = keyPair.getPublic().getEncoded();
		var dto = new CreateDeviceDto();
		dto.id = deviceId;
		dto.name = deviceNameField.getText();
		dto.publicKey = BaseEncoding.base64Url().omitPadding().encode(deviceKey);
		var json = GSON.toJson(dto); // TODO: do we want to keep GSON? doesn't support records -.-
		var request = HttpRequest.newBuilder(keyUri) //
				.header("Authorization", "Bearer " + bearerToken) //
				.header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)) //
				.build();
		httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding()) //
				.thenAcceptAsync(this::registrationSucceeded, Platform::runLater) //
				.exceptionallyAsync(this::registrationFailed, Platform::runLater);
	}

	private void registrationSucceeded(HttpResponse<Void> voidHttpResponse) {
		LOG.info("Registered!");
		result.cancel(true); // TODO: show visual feedback "please wait for device authorization"
	}

	private Void registrationFailed(Throwable cause) {
		result.interacted(HubKeyLoadingModule.HubLoadingResult.FAILED);
		LOG.error("Key retrieval failed", cause);
		// TODO errorComponent.cause(cause).window(window).build().showErrorScene();
		return null;
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

	/* Getter */

	public String getUserName() {
		return jwt.getClaim("email").asString();
	}


}

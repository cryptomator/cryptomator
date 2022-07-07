package org.cryptomator.ui.keyloading.hub;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nimbusds.jose.JWEObject;
import dagger.Lazy;
import org.cryptomator.common.settings.DeviceKey;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class RegisterDeviceController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RegisterDeviceController.class);
	private static final Gson GSON = new GsonBuilder().setLenient().create();

	private final Stage window;
	private final HubConfig hubConfig;
	private final String bearerToken;
	private final Lazy<Scene> registerSuccessScene;
	private final Lazy<Scene> registerFailedScene;
	private final String deviceId;
	private final P384KeyPair keyPair;
	private final CompletableFuture<JWEObject> result;
	private final DecodedJWT jwt;
	private final HttpClient httpClient;

	public TextField deviceNameField;

	@Inject
	public RegisterDeviceController(@KeyLoading Stage window, ExecutorService executor, HubConfig hubConfig, @Named("deviceId") String deviceId, DeviceKey deviceKey, CompletableFuture<JWEObject> result, @Named("bearerToken") AtomicReference<String> bearerToken, @FxmlScene(FxmlFile.HUB_REGISTER_SUCCESS) Lazy<Scene> registerSuccessScene, @FxmlScene(FxmlFile.HUB_REGISTER_FAILED) Lazy<Scene> registerFailedScene) {
		this.window = window;
		this.hubConfig = hubConfig;
		this.deviceId = deviceId;
		this.keyPair = Objects.requireNonNull(deviceKey.get());
		this.result = result;
		this.bearerToken = Objects.requireNonNull(bearerToken.get());
		this.registerSuccessScene = registerSuccessScene;
		this.registerFailedScene = registerFailedScene;
		this.jwt = JWT.decode(this.bearerToken);
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.httpClient = HttpClient.newBuilder().executor(executor).build();
	}

	public void initialize() {
		deviceNameField.setText(determineHostname());
	}

	private String determineHostname() {
		try {
			var hostName = InetAddress.getLocalHost().getHostName();
			return Objects.requireNonNullElse(hostName, "");
		} catch (IOException e) {
			return "";
		}
	}

	@FXML
	public void register() {
		var keyUri = URI.create(hubConfig.devicesResourceUrl + deviceId);
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
				.handleAsync((response, throwable) -> {
					if (response != null) {
						this.registrationSucceeded(response);
					} else {
						this.registrationFailed(throwable);
					}
					return null;
				}, Platform::runLater);
	}

	private void registrationSucceeded(HttpResponse<Void> voidHttpResponse) {
		LOG.debug("Device registration for hub instance {} successful.", hubConfig.authSuccessUrl);
		window.setScene(registerSuccessScene.get());
	}

	private void registrationFailed(Throwable cause) {
		LOG.warn("Device registration failed.", cause);
		window.setScene(registerFailedScene.get());
		result.completeExceptionally(cause);
	}

	@FXML
	public void close() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		result.cancel(true);
	}

	/* Getter */

	public String getUserName() {
		return jwt.getClaim("email").asString();
	}


}

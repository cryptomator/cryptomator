package org.cryptomator.ui.keyloading.hub;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class RegisterDeviceController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RegisterDeviceController.class);
	private static final ObjectMapper JSON = new ObjectMapper().setDefaultLeniency(true);
	private static final List<Integer> EXPECTED_RESPONSE_CODES = List.of(201, 409);

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
	private final BooleanProperty deviceNameAlreadyExists = new SimpleBooleanProperty(false);

	public TextField deviceNameField;
	public Button registerBtn;

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
		this.httpClient = HttpClient.newBuilder().proxy(ProxySelector.getDefault()).version(HttpClient.Version.HTTP_1_1).executor(executor).build();
	}

	public void initialize() {
		deviceNameField.setText(determineHostname());
		deviceNameField.textProperty().addListener(observable -> deviceNameAlreadyExists.set(false));
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
		deviceNameAlreadyExists.set(false);
		registerBtn.setContentDisplay(ContentDisplay.LEFT);
		registerBtn.setDisable(true);

		var keyUri = URI.create(hubConfig.devicesResourceUrl + deviceId);
		var deviceKey = keyPair.getPublic().getEncoded();
		var dto = new CreateDeviceDto(deviceId, deviceNameField.getText(), BaseEncoding.base64Url().omitPadding().encode(deviceKey));
		var json = toJson(dto);
		var request = HttpRequest.newBuilder(keyUri) //
				.header("Authorization", "Bearer " + bearerToken) //
				.header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)) //
				.build();
		httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding()) //
				.thenApply(response -> {
					if (EXPECTED_RESPONSE_CODES.contains(response.statusCode())) {
						return response;
					} else {
						throw new RuntimeException("Server answered with unexpected status code " + response.statusCode());
					}
				}).handleAsync((response, throwable) -> {
					if (response != null) {
						this.handleResponse(response);
					} else {
						this.registrationFailed(throwable);
					}
					return null;
				}, Platform::runLater);
	}

	private String toJson(CreateDeviceDto dto) {
		try {
			return JSON.writer().writeValueAsString(dto);
		} catch (JacksonException e) {
			throw new IllegalStateException("Failed to serialize DTO", e);
		}
	}

	private void handleResponse(HttpResponse<Void> voidHttpResponse) {
		assert EXPECTED_RESPONSE_CODES.contains(voidHttpResponse.statusCode());

		if (voidHttpResponse.statusCode() == 409) {
			deviceNameAlreadyExists.set(true);
			registerBtn.setContentDisplay(ContentDisplay.TEXT_ONLY);
			registerBtn.setDisable(false);
		} else {
			LOG.debug("Device registration for hub instance {} successful.", hubConfig.authSuccessUrl);
			window.setScene(registerSuccessScene.get());
		}
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


	//--- Getters & Setters

	public BooleanProperty deviceNameAlreadyExistsProperty() {
		return deviceNameAlreadyExists;
	}

	public boolean getDeviceNameAlreadyExists() {
		return deviceNameAlreadyExists.get();
	}


}

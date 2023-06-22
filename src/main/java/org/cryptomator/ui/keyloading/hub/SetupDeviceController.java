package org.cryptomator.ui.keyloading.hub;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
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
import org.jetbrains.annotations.Nullable;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class SetupDeviceController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(SetupDeviceController.class);
	private static final Gson GSON = new GsonBuilder().setLenient().create();

	private final Stage window;
	private final HubConfig hubConfig;
	private final String bearerToken;
	private final Lazy<Scene> registerSuccessScene;
	private final Lazy<Scene> registerFailedScene;
	private final String deviceId;
	private final P384KeyPair deviceKeyPair;
	private final CompletableFuture<ReceivedKey> result;
	private final DecodedJWT jwt;
	private final HttpClient httpClient;
	private final BooleanProperty deviceNameAlreadyExists = new SimpleBooleanProperty(false);

	public TextField setupCodeField;
	public TextField deviceNameField;
	public Button registerBtn;

	@Inject
	public SetupDeviceController(@KeyLoading Stage window, ExecutorService executor, HubConfig hubConfig, @Named("deviceId") String deviceId, DeviceKey deviceKey, CompletableFuture<ReceivedKey> result, @Named("bearerToken") AtomicReference<String> bearerToken, @FxmlScene(FxmlFile.HUB_REGISTER_SUCCESS) Lazy<Scene> registerSuccessScene, @FxmlScene(FxmlFile.HUB_REGISTER_FAILED) Lazy<Scene> registerFailedScene) {
		this.window = window;
		this.hubConfig = hubConfig;
		this.deviceId = deviceId;
		this.deviceKeyPair = Objects.requireNonNull(deviceKey.get());
		this.result = result;
		this.bearerToken = Objects.requireNonNull(bearerToken.get());
		this.registerSuccessScene = registerSuccessScene;
		this.registerFailedScene = registerFailedScene;
		this.jwt = JWT.decode(this.bearerToken);
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).executor(executor).build();
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
		setupCodeField.setDisable(true);
		deviceNameField.setDisable(true);
		deviceNameAlreadyExists.set(false);
		registerBtn.setContentDisplay(ContentDisplay.LEFT);
		registerBtn.setDisable(true);

		var apiRootUrl = URI.create(hubConfig.devicesResourceUrl + "/..").normalize(); // TODO: add url to vault config file, only use this as a fallback for legacy vaults
		var deviceUri = URI.create(hubConfig.devicesResourceUrl + deviceId);
		var deviceKey = deviceKeyPair.getPublic().getEncoded();

		var userReq = HttpRequest.newBuilder(apiRootUrl.resolve("users/me")) //
				.GET() //
				.header("Authorization", "Bearer " + bearerToken) //
				.header("Content-Type", "application/json") //
				.build();
		httpClient.sendAsync(userReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)) //
				.thenApply(response -> {
					if (response.statusCode() == 200) {
						return GSON.fromJson(response.body(), UserDto.class);
					} else {
						throw new RuntimeException("Server answered with unexpected status code " + response.statusCode());
					}
				}).thenApply(user -> {
					try {
						var userKey = JWEHelper.decryptUserKey(JWEObject.parse(user.privateKey), setupCodeField.getText());
						return JWEHelper.encryptUserKey(userKey, deviceKeyPair.getPublic());
					} catch (ParseException e) {
						throw new RuntimeException("Server answered with unparsable user key", e);
					}
				}).thenCompose(jwe -> {
					var dto = new CreateDeviceDto();
					dto.id = deviceId;
					dto.name = deviceNameField.getText();
					dto.publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(deviceKey);
					dto.userKey = jwe.serialize();
					dto.creationTime = Instant.now().toString();
					dto.lastSeenTime = Instant.now().toString();
					var json = GSON.toJson(dto);
					var putDeviceReq = HttpRequest.newBuilder(deviceUri) //
							.PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)) //
							.header("Authorization", "Bearer " + bearerToken) //
							.header("Content-Type", "application/json") //
							.build();
					return httpClient.sendAsync(putDeviceReq, HttpResponse.BodyHandlers.discarding());
				}).handleAsync((response, throwable) -> {
					if (response != null) {
						this.handleResponse(response);
					} else {
						this.registrationFailed(throwable);
					}
					return null;
				}, Platform::runLater);
	}

	private void handleResponse(HttpResponse<Void> response) {
		if (response.statusCode() == 201) {
			LOG.debug("Device registration for hub instance {} successful.", hubConfig.authSuccessUrl);
			window.setScene(registerSuccessScene.get());
		} else if (response.statusCode() == 409) {
			deviceNameAlreadyExists.set(true);
			registerBtn.setContentDisplay(ContentDisplay.TEXT_ONLY);
			registerBtn.setDisable(false);
		} else {
			registrationFailed(new IllegalStateException("Unexpected http status code " + response.statusCode()));
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


	private class UserDto {
		public String id;
		public String name;
		public @Nullable String publicKey;
		public @Nullable String privateKey;
		public @Nullable String setupCode;
	}
}

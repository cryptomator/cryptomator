package org.cryptomator.ui.keyloading.hub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import javafx.beans.binding.Bindings;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class RegisterDeviceController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(RegisterDeviceController.class);
	private static final ObjectMapper JSON = new ObjectMapper().setDefaultLeniency(true);
	private static final Duration REQ_TIMEOUT = Duration.ofSeconds(10);

	private final Stage window;
	private final HubConfig hubConfig;
	private final String bearerToken;
	private final Lazy<Scene> registerSuccessScene;
	private final Lazy<Scene> registerFailedScene;
	private final String deviceId;
	private final P384KeyPair deviceKeyPair;
	private final CompletableFuture<ReceivedKey> result;
	private final HttpClient httpClient;

	private final BooleanProperty deviceNameAlreadyExists = new SimpleBooleanProperty(false);
	private final BooleanProperty invalidSetupCode = new SimpleBooleanProperty(false);
	private final BooleanProperty workInProgress = new SimpleBooleanProperty(false);
	public TextField setupCodeField;
	public TextField deviceNameField;
	public Button registerBtn;

	@Inject
	public RegisterDeviceController(@KeyLoading Stage window, ExecutorService executor, HubConfig hubConfig, @Named("deviceId") String deviceId, DeviceKey deviceKey, CompletableFuture<ReceivedKey> result, @Named("bearerToken") AtomicReference<String> bearerToken, @FxmlScene(FxmlFile.HUB_REGISTER_SUCCESS) Lazy<Scene> registerSuccessScene, @FxmlScene(FxmlFile.HUB_REGISTER_FAILED) Lazy<Scene> registerFailedScene) {
		this.window = window;
		this.hubConfig = hubConfig;
		this.deviceId = deviceId;
		this.deviceKeyPair = Objects.requireNonNull(deviceKey.get());
		this.result = result;
		this.bearerToken = Objects.requireNonNull(bearerToken.get());
		this.registerSuccessScene = registerSuccessScene;
		this.registerFailedScene = registerFailedScene;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).executor(executor).build();
	}

	public void initialize() {
		deviceNameField.setText(determineHostname());
		deviceNameField.textProperty().addListener(observable -> deviceNameAlreadyExists.set(false));
		deviceNameField.disableProperty().bind(workInProgress);
		setupCodeField.textProperty().addListener(observable -> invalidSetupCode.set(false));
		setupCodeField.disableProperty().bind(workInProgress);
		var missingSetupCode = setupCodeField.textProperty().isEmpty();
		var missingDeviceName = deviceNameField.textProperty().isEmpty();
		registerBtn.disableProperty().bind(workInProgress.or(missingSetupCode).or(missingDeviceName));
		registerBtn.contentDisplayProperty().bind(Bindings.when(workInProgress).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY));
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
		workInProgress.set(true);

		var apiRootUrl = hubConfig.getApiBaseUrl();
		var deviceKey = deviceKeyPair.getPublic().getEncoded();

		var userReq = HttpRequest.newBuilder(apiRootUrl.resolve("users/me")) //
				.GET() //
				.timeout(REQ_TIMEOUT) //
				.header("Authorization", "Bearer " + bearerToken) //
				.header("Content-Type", "application/json") //
				.build();
		httpClient.sendAsync(userReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)) //
				.thenApply(response -> {
					if (response.statusCode() == 200) {
						var dto = fromJson(response.body());
						return Objects.requireNonNull(dto, "null or empty response body");
					} else {
						throw new RuntimeException("Server answered with unexpected status code " + response.statusCode());
					}
				}).thenApply(user -> {
					try {
						assert user.privateKey != null; // api/vaults/{v}/user-tokens/me would have returned 403, if user wasn't fully set up yet
						var userKey = JWEHelper.decryptUserKey(JWEObject.parse(user.privateKey), setupCodeField.getText());
						return JWEHelper.encryptUserKey(userKey, deviceKeyPair.getPublic());
					} catch (ParseException e) {
						throw new RuntimeException("Server answered with unparsable user key", e);
					}
				}).thenCompose(jwe -> {
					var now = Instant.now().toString();
					var dto = new CreateDeviceDto(deviceId, deviceNameField.getText(), BaseEncoding.base64().encode(deviceKey), "DESKTOP", jwe.serialize(), now);
					var json = toJson(dto);
					var deviceUri = apiRootUrl.resolve("devices/" + deviceId);
					var putDeviceReq = HttpRequest.newBuilder(deviceUri) //
							.PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)) //
							.timeout(REQ_TIMEOUT) //
							.header("Authorization", "Bearer " + bearerToken) //
							.header("Content-Type", "application/json") //
							.build();
					return httpClient.sendAsync(putDeviceReq, HttpResponse.BodyHandlers.discarding());
				}).whenCompleteAsync((response, throwable) -> {
					if (response != null) {
						this.handleResponse(response);
					} else {
						this.setupFailed(throwable);
					}
					workInProgress.set(false);
				}, Platform::runLater);
	}

	private UserDto fromJson(String json) {
		try {
			return JSON.reader().readValue(json, UserDto.class);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to deserialize DTO", e);
		}
	}

	private String toJson(CreateDeviceDto dto) {
		try {
			return JSON.writer().writeValueAsString(dto);
		} catch (JacksonException e) {
			throw new IllegalStateException("Failed to serialize DTO", e);
		}
	}

	private void handleResponse(HttpResponse<Void> response) {
		if (response.statusCode() == 201) {
			LOG.debug("Device registration for hub instance {} successful.", hubConfig.authSuccessUrl);
			window.setScene(registerSuccessScene.get());
		} else if (response.statusCode() == 409) {
			deviceNameAlreadyExists.set(true);
		} else {
			setupFailed(new IllegalStateException("Unexpected http status code " + response.statusCode()));
		}
	}

	private void setupFailed(Throwable cause) {
		switch (cause) {
			case CompletionException e when e.getCause() instanceof JWEHelper.InvalidJweKeyException -> invalidSetupCode.set(true);
			default -> {
				LOG.warn("Device setup failed.", cause);
				window.setScene(registerFailedScene.get());
				result.completeExceptionally(cause);
			}
		}
	}

	@FXML
	public void close() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		result.cancel(true);
	}

	//--- Getters & Setters

	public BooleanProperty deviceNameAlreadyExistsProperty() {
		return deviceNameAlreadyExists;
	}

	public boolean getDeviceNameAlreadyExists() {
		return deviceNameAlreadyExists.get();
	}

	public BooleanProperty invalidSetupCodeProperty() {
		return invalidSetupCode;
	}

	public boolean isInvalidSetupCode() {
		return invalidSetupCode.get();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record UserDto(String id, String name, String publicKey, String privateKey, String setupCode) {}

	private record CreateDeviceDto(@JsonProperty(required = true) String id, //
								   @JsonProperty(required = true) String name, //
								   @JsonProperty(required = true) String publicKey, //
								   @JsonProperty(required = true, defaultValue = "DESKTOP") String type, //
								   @JsonProperty(required = true) String userPrivateKey, //
								   @JsonProperty(required = true) String creationTime) {}
}

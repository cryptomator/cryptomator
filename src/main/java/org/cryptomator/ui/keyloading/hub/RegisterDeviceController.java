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
import java.io.InterruptedIOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
	private final Lazy<Scene> deviceAlreadyExistsScene;
	private final String deviceId;
	private final P384KeyPair deviceKeyPair;
	private final CompletableFuture<ReceivedKey> result;
	private final HttpClient httpClient;

	private final BooleanProperty invalidSetupCode = new SimpleBooleanProperty(false);
	private final BooleanProperty workInProgress = new SimpleBooleanProperty(false);
	public TextField setupCodeField;
	public TextField deviceNameField;
	public Button registerBtn;

	@Inject
	public RegisterDeviceController(@KeyLoading Stage window, ExecutorService executor, HubConfig hubConfig, @Named("deviceId") String deviceId, DeviceKey deviceKey, CompletableFuture<ReceivedKey> result, @Named("bearerToken") AtomicReference<String> bearerToken, @FxmlScene(FxmlFile.HUB_REGISTER_SUCCESS) Lazy<Scene> registerSuccessScene, @FxmlScene(FxmlFile.HUB_REGISTER_FAILED) Lazy<Scene> registerFailedScene, @FxmlScene(FxmlFile.HUB_REGISTER_DEVICE_ALREADY_EXISTS) Lazy<Scene> deviceAlreadyExistsScene) {
		this.window = window;
		this.hubConfig = hubConfig;
		this.deviceId = deviceId;
		this.deviceKeyPair = Objects.requireNonNull(deviceKey.get());
		this.result = result;
		this.bearerToken = Objects.requireNonNull(bearerToken.get());
		this.registerSuccessScene = registerSuccessScene;
		this.registerFailedScene = registerFailedScene;
		this.deviceAlreadyExistsScene = deviceAlreadyExistsScene;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).executor(executor).build();
	}

	public void initialize() {
		deviceNameField.setText(determineHostname());
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


		var userReq = HttpRequest.newBuilder(hubConfig.URIs.API."users/me") //
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
						assert user.privateKey != null && user.publicKey != null; // api/vaults/{v}/user-tokens/me would have returned 403, if user wasn't fully set up yet
						var userPublicKey = JWEHelper.decodeECPublicKey(Base64.getDecoder().decode(user.publicKey));
						migrateLegacyDevices(userPublicKey); // TODO: remove eventually, when most users have migrated to Hub 1.3.x or newer
						var userKey = JWEHelper.decryptUserKey(JWEObject.parse(user.privateKey), setupCodeField.getText());
						return JWEHelper.encryptUserKey(userKey, deviceKeyPair.getPublic());
					} catch (ParseException | JWEHelper.KeyDecodeFailedException e) {
						throw new RuntimeException("Server answered with unparsable user key", e);
					}
				}).thenCompose(jwe -> {
					var now = Instant.now().toString();
					var dto = new CreateDeviceDto(deviceId, deviceNameField.getText(), BaseEncoding.base64().encode(deviceKeyPair.getPublic().getEncoded()), "DESKTOP", jwe.serialize(), now);
					var json = toJson(dto);
					var deviceUri = hubConfig.URIs.API."devices/\{deviceId}";
					var putDeviceReq = HttpRequest.newBuilder(deviceUri) //
							.PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)) //
							.timeout(REQ_TIMEOUT) //
							.header("Authorization", "Bearer " + bearerToken) //
							.header("Content-Type", "application/json") //
							.build();
					return httpClient.sendAsync(putDeviceReq, HttpResponse.BodyHandlers.discarding());
				}).whenCompleteAsync((response, throwable) -> {
					if (response != null) {
						this.handleRegisterDeviceResponse(response);
					} else {
						this.setupFailed(throwable);
					}
					workInProgress.set(false);
				}, Platform::runLater);
	}

	private void migrateLegacyDevices(ECPublicKey userPublicKey) {
		try {
			// GET legacy access tokens
			var getUri = hubConfig.URIs.API."devices/\{deviceId}/legacy-access-tokens";
			var getReq = HttpRequest.newBuilder(getUri).GET().timeout(REQ_TIMEOUT).header("Authorization", "Bearer " + bearerToken).build();
			var getRes = httpClient.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (getRes.statusCode() != 200) {
				LOG.debug("GET {} resulted in status code {}. Skipping migration.", getUri, getRes.statusCode());
				return;
			}
			Map<String, String> legacyAccessTokens = JSON.readerForMapOf(String.class).readValue(getRes.body());
			if (legacyAccessTokens.isEmpty()) {
				return; // no migration required
			}

			// POST new access tokens
			Map<String, String> newAccessTokens = legacyAccessTokens.entrySet().stream().<Map.Entry<String, String>>mapMulti((entry, consumer) -> {
				try (var vaultKey = JWEHelper.decryptVaultKey(JWEObject.parse(entry.getValue()), deviceKeyPair.getPrivate())) {
					var newAccessToken = JWEHelper.encryptVaultKey(vaultKey, userPublicKey).serialize();
					consumer.accept(Map.entry(entry.getKey(), newAccessToken));
				} catch (ParseException | JWEHelper.InvalidJweKeyException e) {
					LOG.warn("Failed to decrypt legacy access token for vault {}. Skipping migration.", entry.getKey());
				}
			}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
			var postUri = hubConfig.URIs.API."users/me/access-tokens";
			var postBody = JSON.writer().writeValueAsString(newAccessTokens);
			var postReq = HttpRequest.newBuilder(postUri).POST(HttpRequest.BodyPublishers.ofString(postBody)).timeout(REQ_TIMEOUT).header("Authorization", "Bearer " + bearerToken).build();
			var postRes = httpClient.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (postRes.statusCode() != 200) {
				throw new IOException(STR."Unexpected response from POST \{postUri}: \{postRes.statusCode()}");
			}
		} catch (IOException e) {
			// log and ignore: this is merely a best-effort attempt of migrating legacy devices. Failure is uncritical as this is merely a convenience feature.
			LOG.error("Legacy Device Migration failed.", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new UncheckedIOException(new InterruptedIOException("Legacy Device Migration interrupted"));
		}
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

	private void handleRegisterDeviceResponse(HttpResponse<Void> response) {
		if (response.statusCode() == 201) {
			LOG.debug("Device registration for hub instance {} successful.", hubConfig.authSuccessUrl);
			window.setScene(registerSuccessScene.get());
		} else if (response.statusCode() == 409) {
			setupFailed(new DeviceAlreadyExistsException());
		} else {
			setupFailed(new IllegalStateException("Unexpected http status code " + response.statusCode()));
		}
	}

	private void setupFailed(Throwable cause) {
		switch (cause) {
			case CompletionException e when e.getCause() instanceof JWEHelper.InvalidJweKeyException -> invalidSetupCode.set(true);
			case DeviceAlreadyExistsException e -> {
				LOG.debug("Device already registered in hub instance {} for different user", hubConfig.authSuccessUrl);
				window.setScene(deviceAlreadyExistsScene.get());
			}
			default -> {
				LOG.warn("Device setup failed.", cause);
				window.setScene(registerFailedScene.get());
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

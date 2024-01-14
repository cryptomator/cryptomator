package org.cryptomator.ui.keyloading.hub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.nimbusds.jose.JWEObject;
import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
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
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class ReceiveKeyController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ReceiveKeyController.class);
	private static final String SCHEME_PREFIX = "hub+";
	private static final ObjectMapper JSON = new ObjectMapper().setDefaultLeniency(true);
	private static final Duration REQ_TIMEOUT = Duration.ofSeconds(10);

	private final Stage window;
	private final HubConfig hubConfig;
	private final String vaultId;
	private final String deviceId;
	private final String bearerToken;
	private final CompletableFuture<ReceivedKey> result;
	private final Lazy<Scene> registerDeviceScene;
	private final Lazy<Scene> legacyRegisterDeviceScene;
	private final Lazy<Scene> unauthorizedScene;
	private final Lazy<Scene> accountInitializationScene;
	private final Lazy<Scene> invalidLicenseScene;
	private final HttpClient httpClient;
	private final StringTemplate.Processor<URI, RuntimeException> API_BASE = this::resolveRelativeToApiBase;

	@Inject
	public ReceiveKeyController(@KeyLoading Vault vault, ExecutorService executor, @KeyLoading Stage window, HubConfig hubConfig, @Named("deviceId") String deviceId, @Named("bearerToken") AtomicReference<String> tokenRef, CompletableFuture<ReceivedKey> result, @FxmlScene(FxmlFile.HUB_REGISTER_DEVICE) Lazy<Scene> registerDeviceScene, @FxmlScene(FxmlFile.HUB_LEGACY_REGISTER_DEVICE) Lazy<Scene> legacyRegisterDeviceScene, @FxmlScene(FxmlFile.HUB_UNAUTHORIZED_DEVICE) Lazy<Scene> unauthorizedScene, @FxmlScene(FxmlFile.HUB_REQUIRE_ACCOUNT_INIT) Lazy<Scene> accountInitializationScene, @FxmlScene(FxmlFile.HUB_INVALID_LICENSE) Lazy<Scene> invalidLicenseScene) {
		this.window = window;
		this.hubConfig = hubConfig;
		this.vaultId = extractVaultId(vault.getVaultConfigCache().getUnchecked().getKeyId()); // TODO: access vault config's JTI directly (requires changes in cryptofs)
		this.deviceId = deviceId;
		this.bearerToken = Objects.requireNonNull(tokenRef.get());
		this.result = result;
		this.registerDeviceScene = registerDeviceScene;
		this.legacyRegisterDeviceScene = legacyRegisterDeviceScene;
		this.unauthorizedScene = unauthorizedScene;
		this.accountInitializationScene = accountInitializationScene;
		this.invalidLicenseScene = invalidLicenseScene;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).executor(executor).build();
	}

	@FXML
	public void initialize() {
		requestDeviceData();
	}

	/**
	 * STEP 2 (Request): GET vault key for this user
	 */
	private void requestVaultMasterkey(String encryptedUserKey) {
		var vaultKeyUri = API_BASE."vaults/\{vaultId}/access-token";
		var request = HttpRequest.newBuilder(vaultKeyUri) //
				.header("Authorization", "Bearer " + bearerToken) //
				.GET() //
				.timeout(REQ_TIMEOUT) //
				.build();
		httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.US_ASCII)) //
				.thenAcceptAsync(response -> receivedVaultMasterkey(encryptedUserKey, response), Platform::runLater) //
				.exceptionally(this::retrievalFailed);
	}

	/**
	 * STEP 2 (Response): GET vault key for this user
	 *
	 * @param response Response
	 */
	private void receivedVaultMasterkey(String encryptedUserKey, HttpResponse<String> response) {
		LOG.debug("GET {} -> Status Code {}", response.request().uri(), response.statusCode());
		switch (response.statusCode()) {
			case 200 -> receivedBothEncryptedKeys(response.body(), encryptedUserKey);
			case 402 -> licenseExceeded();
			case 403, 410 -> accessNotGranted(); // or vault has been archived, effectively disallowing access - TODO: add specific dialog?
			case 449 -> accountInitializationRequired();
			case 404 -> requestLegacyAccessToken();
			default -> throw new IllegalStateException("Unexpected response " + response.statusCode());
		}
	}

	/**
	 * STEP 1 (Request): GET user key for this device
	 */
	private void requestDeviceData() {
		var deviceUri = API_BASE."devices/\{deviceId}";
		var request = HttpRequest.newBuilder(deviceUri) //
				.header("Authorization", "Bearer " + bearerToken) //
				.GET() //
				.timeout(REQ_TIMEOUT) //
				.build();
		httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)) //
				.thenAcceptAsync(this::receivedDeviceData, Platform::runLater) //
				.exceptionally(this::retrievalFailed);
	}

	/**
	 * STEP 1 (Response): GET user key for this device
	 *
	 * @param response Response
	 */
	private void receivedDeviceData(HttpResponse<String> response) {
		LOG.debug("GET {} -> Status Code {}", response.request().uri(), response.statusCode());
		try {
			switch (response.statusCode()) {
				case 200 -> {
					var device = JSON.reader().readValue(response.body(), DeviceDto.class);
					requestVaultMasterkey(device.userPrivateKey);
				}
				case 404 -> needsDeviceRegistration(); // TODO: using the setup code, we can theoretically immediately unlock
				default -> throw new IllegalStateException("Unexpected response " + response.statusCode());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void needsDeviceRegistration() {
		window.setScene(registerDeviceScene.get());
	}

	private void receivedBothEncryptedKeys(String encryptedVaultKey, String encryptedUserKey) {
		try {
			var vaultKeyJwe = JWEObject.parse(encryptedVaultKey);
			var userKeyJwe = JWEObject.parse(encryptedUserKey);
			result.complete(ReceivedKey.vaultKeyAndUserKey(vaultKeyJwe, userKeyJwe));
			window.close();
		} catch (ParseException e) {
			retrievalFailed(e);
		}
	}

	/**
	 * LEGACY FALLBACK (Request): GET the legacy access token from Hub 1.x
	 */
	@Deprecated
	private void requestLegacyAccessToken() {
		var legacyAccessTokenUri = API_BASE."vaults/\{vaultId}/keys/\{deviceId}";
		var request = HttpRequest.newBuilder(legacyAccessTokenUri) //
				.header("Authorization", "Bearer " + bearerToken) //
				.GET() //
				.timeout(REQ_TIMEOUT) //
				.build();
		httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.US_ASCII)) //
				.thenAcceptAsync(this::receivedLegacyAccessTokenResponse, Platform::runLater) //
				.exceptionally(this::retrievalFailed);
	}

	/**
	 * LEGACY FALLBACK (Response)
	 *
	 * @param response Response
	 */
	@Deprecated
	private void receivedLegacyAccessTokenResponse(HttpResponse<String> response) {
		try {
			switch (response.statusCode()) {
				case 200 -> receivedLegacyAccessTokenSuccess(response.body());
				case 402 -> licenseExceeded();
				case 403, 410 -> accessNotGranted(); // or vault has been archived, effectively disallowing access
				case 404 -> needsLegacyDeviceRegistration();
				default -> throw new IOException("Unexpected response " + response.statusCode());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Deprecated
	private void receivedLegacyAccessTokenSuccess(String rawToken) throws IOException {
		try {
			var token = JWEObject.parse(rawToken);
			result.complete(ReceivedKey.legacyDeviceKey(token));
			window.close();
		} catch (ParseException e) {
			throw new IOException("Failed to parse JWE", e);
		}
	}

	private void licenseExceeded() {
		window.setScene(invalidLicenseScene.get());
	}

	@Deprecated
	private void needsLegacyDeviceRegistration() {
		window.setScene(legacyRegisterDeviceScene.get());
	}

	private void accessNotGranted() {
		window.setScene(unauthorizedScene.get());
	}

	private void accountInitializationRequired() {
		window.setScene(accountInitializationScene.get());
	}

	private Void retrievalFailed(Throwable cause) {
		result.completeExceptionally(cause);
		return null;
	}

	@FXML
	public void cancel() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		result.cancel(true);
	}

	private static URI appendPath(URI base, String path) {
		try {
			var newPath = base.getPath() + path;
			return new URI(base.getScheme(), base.getAuthority(), newPath, base.getQuery(), base.getFragment());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Can't append '" + path + "' to URI: " + base, e);
		}
	}

	private URI resolveRelativeToApiBase(StringTemplate template) {
		var path = template.interpolate();
		var relPath = path.startsWith("/") ? path.substring(1) : path;
		return hubConfig.getApiBaseUrl().resolve(relPath);
	}

	private static String extractVaultId(URI vaultKeyUri) {
		assert vaultKeyUri.getScheme().startsWith(SCHEME_PREFIX);
		var path = vaultKeyUri.getPath();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record DeviceDto(@JsonProperty(value = "userPrivateKey", required = true) String userPrivateKey) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ConfigDto(@JsonProperty(value = "apiLevel") int apiLevel) {}
}

package org.cryptomator.ui.keyloading.hub;

import com.nimbusds.jose.JWEObject;
import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class ReceiveKeyController implements FxController {

	private static final String SCHEME_PREFIX = "hub+";

	private final Stage window;
	private final String deviceId;
	private final String bearerToken;
	private final CompletableFuture<JWEObject> result;
	private final Lazy<Scene> registerDeviceScene;
	private final Lazy<Scene> unauthorizedScene;
	private final URI vaultBaseUri;
	private final Lazy<Scene> licenseExceededScene;
	private final HttpClient httpClient;

	@Inject
	public ReceiveKeyController(@KeyLoading Vault vault, ExecutorService executor, @KeyLoading Stage window, @Named("deviceId") String deviceId, @Named("bearerToken") AtomicReference<String> tokenRef, CompletableFuture<JWEObject> result, @FxmlScene(FxmlFile.HUB_REGISTER_DEVICE) Lazy<Scene> registerDeviceScene, @FxmlScene(FxmlFile.HUB_UNAUTHORIZED_DEVICE) Lazy<Scene> unauthorizedScene, @FxmlScene(FxmlFile.HUB_LICENSE_EXCEEDED) Lazy<Scene> licenseExceededScene) {
		this.window = window;
		this.deviceId = deviceId;
		this.bearerToken = Objects.requireNonNull(tokenRef.get());
		this.result = result;
		this.registerDeviceScene = registerDeviceScene;
		this.unauthorizedScene = unauthorizedScene;
		this.vaultBaseUri = getVaultBaseUri(vault);
		this.licenseExceededScene = licenseExceededScene;
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.httpClient = HttpClient.newBuilder().executor(executor).build();
	}

	@FXML
	public void initialize() {
		var keyUri = appendPath(vaultBaseUri, "/keys/" + deviceId);
		var request = HttpRequest.newBuilder(keyUri) //
				.header("Authorization", "Bearer " + bearerToken) //
				.GET() //
				.build();
		httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()) //
				.thenAcceptAsync(this::loadedExistingKey, Platform::runLater) //
				.exceptionally(this::retrievalFailed);
	}

	private void loadedExistingKey(HttpResponse<InputStream> response) {
		try {
			switch (response.statusCode()) {
				case 200 -> retrievalSucceeded(response);
				case 402 -> hubLicenseExceeded();
				case 403 -> accessNotGranted();
				case 404 -> needsDeviceRegistration();
				default -> throw new IOException("Unexpected response " + response.statusCode());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void retrievalSucceeded(HttpResponse<InputStream> response) throws IOException {
		try {
			var string = HttpHelper.readBody(response);
			result.complete(JWEObject.parse(string));
			window.close();
		} catch (ParseException e) {
			throw new IOException("Failed to parse JWE", e);
		}
	}

	private void hubLicenseExceeded() {
		window.setScene(licenseExceededScene.get());
	}

	private void needsDeviceRegistration() {
		window.setScene(registerDeviceScene.get());
	}

	private void accessNotGranted() {
		window.setScene(unauthorizedScene.get());
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

	private static URI getVaultBaseUri(Vault vault) {
		try {
			var kid = vault.getVaultConfigCache().get().getKeyId();
			assert kid.getScheme().startsWith(SCHEME_PREFIX);
			var hubUriScheme = kid.getScheme().substring(SCHEME_PREFIX.length());
			return new URI(hubUriScheme, kid.getSchemeSpecificPart(), kid.getFragment());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI constructed from params known to be valid", e);
		}
	}
}

package org.cryptomator.ui.keyloading.hub;

import com.google.common.io.BaseEncoding;
import com.nimbusds.jose.JWEObject;
import dagger.Lazy;
import org.cryptomator.common.settings.DeviceKey;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.cryptolib.common.MessageDigestSupplier;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.UserInteractionLock;
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class ReceiveKeyController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ReceiveKeyController.class);
	private static final String SCHEME_PREFIX = "hub+";

	private final Stage window;
	private final P384KeyPair keyPair;
	private final String bearerToken;
	private final AtomicReference<JWEObject> jweRef;
	private final UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result;
	private final Lazy<Scene> registerDeviceScene;
	private final Lazy<Scene> unauthorizedScene;
	private final ErrorComponent.Builder errorComponent;
	private final URI vaultBaseUri;
	private final HttpClient httpClient;

	@Inject
	public ReceiveKeyController(@KeyLoading Vault vault, ExecutorService executor, @KeyLoading Stage window, DeviceKey deviceKey, @Named("bearerToken") AtomicReference<String> tokenRef, AtomicReference<JWEObject> jweRef, UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result, @FxmlScene(FxmlFile.HUB_REGISTER_DEVICE) Lazy<Scene> registerDeviceScene, @FxmlScene(FxmlFile.HUB_UNAUTHORIZED_DEVICE) Lazy<Scene> unauthorizedScene, ErrorComponent.Builder errorComponent) {
		this.window = window;
		this.keyPair = Objects.requireNonNull(deviceKey.get());
		this.bearerToken = Objects.requireNonNull(tokenRef.get());
		this.jweRef = jweRef;
		this.result = result;
		this.registerDeviceScene = registerDeviceScene;
		this.unauthorizedScene = unauthorizedScene;
		this.errorComponent = errorComponent;
		this.vaultBaseUri = getVaultBaseUri(vault);
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.httpClient = HttpClient.newBuilder().executor(executor).build();
	}

	@FXML
	public void initialize() {
		var deviceKey = keyPair.getPublic().getEncoded();
		var hashedKey = MessageDigestSupplier.SHA256.get().digest(deviceKey);
		var deviceId = BaseEncoding.base16().encode(hashedKey);
		var keyUri = appendPath(vaultBaseUri, "/keys/" + deviceId);
		var request = HttpRequest.newBuilder(keyUri) //
				.header("Authorization", "Bearer " + bearerToken) //
				.GET() //
				.build();
		httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()) //
				.whenCompleteAsync(this::loadedExistingKey, Platform::runLater);
	}

	private void loadedExistingKey(HttpResponse<InputStream> response, Throwable error) {
		if (error != null) {
			retrievalFailed(error);
		} else {
			switch (response.statusCode()) {
				case 200 -> retrievalSucceeded(response);
				case 403 -> accessNotGranted();
				case 404 -> needsDeviceRegistration();
				default -> retrievalFailed(new IOException("Unexpected response " + response.statusCode()));
			}
		}
	}

	private void retrievalSucceeded(HttpResponse<InputStream> response) {
		try {
			var string = HttpHelper.readBody(response);
			jweRef.set(JWEObject.parse(string));
			result.interacted(HubKeyLoadingModule.HubLoadingResult.SUCCESS);
			window.close();
		} catch (ParseException | IOException e) {
			retrievalFailed(e);
		}
	}

	private void needsDeviceRegistration() {
		window.setScene(registerDeviceScene.get());
	}

	private void accessNotGranted() {
		window.setScene(unauthorizedScene.get());
	}

	private void retrievalFailed(Throwable cause) {
		result.interacted(HubKeyLoadingModule.HubLoadingResult.FAILED);
		LOG.error("Key retrieval failed", cause);
		errorComponent.cause(cause).window(window).build().showErrorScene();
	}

	@FXML
	public void cancel() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		// if not already interacted, mark this workflow as cancelled:
		if (result.awaitingInteraction().get()) {
			LOG.debug("Authorization cancelled by user.");
			result.interacted(HubKeyLoadingModule.HubLoadingResult.CANCELLED);
		}
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

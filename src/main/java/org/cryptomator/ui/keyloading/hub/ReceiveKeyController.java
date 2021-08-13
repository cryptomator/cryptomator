package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dagger.Lazy;
import org.cryptomator.common.vaults.Vault;
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
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
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
import java.security.KeyPair;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class ReceiveKeyController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ReceiveKeyController.class);
	private static final String SCHEME_PREFIX = "hub+";

	private final Stage window;
	private final String bearerToken;
	private final AtomicReference<EciesParams> eciesParamsRef;
	private final UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result;
	private final Lazy<Scene> registerDeviceScene;
	private final ErrorComponent.Builder errorComponent;
	private final URI vaultBaseUri;
	private final HttpClient httpClient;

	@Inject
	public ReceiveKeyController(@KeyLoading Vault vault, ExecutorService executor, @KeyLoading Stage window, AtomicReference<KeyPair> keyPairRef, @Named("bearerToken") AtomicReference<String> tokenRef, AtomicReference<EciesParams> eciesParamsRef, UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result, @FxmlScene(FxmlFile.HUB_REGISTER_DEVICE) Lazy<Scene> registerDeviceScene, ErrorComponent.Builder errorComponent) {
		this.window = window;
		this.bearerToken = Objects.requireNonNull(tokenRef.get());
		this.eciesParamsRef = eciesParamsRef;
		this.result = result;
		this.registerDeviceScene = registerDeviceScene;
		this.errorComponent = errorComponent;
		this.vaultBaseUri = getVaultBaseUri(vault);
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
		this.httpClient = HttpClient.newBuilder().executor(executor).build();
	}

	@FXML
	public void initialize() {
		var keyUri = appendPath(vaultBaseUri, "/keys/desktop-app"); // TODO use actual device id
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
			var json = HttpHelper.parseBody(response);
			Preconditions.checkArgument(json.isJsonObject());
			Preconditions.checkArgument(json.getAsJsonObject().has("device_specific_masterkey"));
			Preconditions.checkArgument(json.getAsJsonObject().has("ephemeral_public_key"));
			var m = json.getAsJsonObject().get("device_specific_masterkey").getAsString();
			var epk = json.getAsJsonObject().get("ephemeral_public_key").getAsString();
			eciesParamsRef.set(new EciesParams(m, epk));
			result.interacted(HubKeyLoadingModule.HubLoadingResult.SUCCESS);
			window.close();
		} catch (IOException | IllegalArgumentException e) {
			retrievalFailed(e);
		}
	}

	private void needsDeviceRegistration() {
		window.setScene(registerDeviceScene.get());
	}

	private void accessNotGranted() {
		LOG.warn("unauthorized device"); // TODO
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
			var kid = vault.getUnverifiedVaultConfig().getKeyId();
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

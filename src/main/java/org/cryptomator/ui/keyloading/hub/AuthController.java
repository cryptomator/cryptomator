package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.UserInteractionLock;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class AuthController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

	private final Application application;
	private final ExecutorService executor;
	private final Stage window;
	private final KeyPair keyPair;
	private final UserInteractionLock<HubKeyLoadingModule.AuthFlow> authFlowLock;
	private final AtomicReference<URI> hubUriRef;
	private final ErrorComponent.Builder errorComponent;
	private final ObjectProperty<URI> redirectUriRef;
	private final BooleanBinding ready;
	private final AuthReceiveTask receiveTask;

	@Inject
	public AuthController(Application application, ExecutorService executor, @KeyLoading Stage window, AtomicReference<KeyPair> keyPairRef, UserInteractionLock<HubKeyLoadingModule.AuthFlow> authFlowLock, AtomicReference<URI> hubUriRef, ErrorComponent.Builder errorComponent) {
		this.application = application;
		this.executor = executor;
		this.window = window;
		this.keyPair = Objects.requireNonNull(keyPairRef.get());
		this.authFlowLock = authFlowLock;
		this.hubUriRef = hubUriRef;
		this.errorComponent = errorComponent;
		this.redirectUriRef = new SimpleObjectProperty<>();
		this.ready = redirectUriRef.isNotNull();
		this.receiveTask = new AuthReceiveTask(redirectUriRef::set);
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void initialize() {
		Preconditions.checkState(hubUriRef.get() != null);
		receiveTask.setOnSucceeded(this::receivedKey);
		receiveTask.setOnFailed(this::keyRetrievalFailed);
		executor.submit(receiveTask);
	}

	private void keyRetrievalFailed(WorkerStateEvent workerStateEvent) {
		LOG.error("Cryptomator Hub login failed with error", receiveTask.getException());
		authFlowLock.interacted(HubKeyLoadingModule.AuthFlow.FAILED);
		errorComponent.cause(receiveTask.getException()).window(window).build().showErrorScene();
	}

	private void receivedKey(WorkerStateEvent workerStateEvent) {
		var authParams = receiveTask.getValue();
		LOG.info("Cryptomator Hub login succeeded: {} encrypted with {}", authParams.getEphemeralPublicKey(), keyPair.getPublic());
		// TODO decrypt and return masterkey
		authFlowLock.interacted(HubKeyLoadingModule.AuthFlow.SUCCESS);
		window.close();
	}

	@FXML
	public void cancel() {
		window.close();
	}

	private void windowClosed(WindowEvent windowEvent) {
		// stop server, if it is still running
		receiveTask.cancel();
		// if not already interacted, mark this workflow as cancelled:
		if (authFlowLock.awaitingInteraction().get()) {
			LOG.debug("Authorization cancelled by user.");
			authFlowLock.interacted(HubKeyLoadingModule.AuthFlow.CANCELLED);
		}
	}

	@FXML
	public void openBrowser() {
		assert ready.get();
		var hubUri = Objects.requireNonNull(hubUriRef.get());
		var redirectUri = Objects.requireNonNull(redirectUriRef.get());
		var sb = new StringBuilder(hubUri.toString());
		sb.append("?redirect_uri=").append(URLEncoder.encode(redirectUri.toString(), StandardCharsets.US_ASCII));
		sb.append("&device_id=").append("desktop-app-3000");
		sb.append("&device_key=").append(BaseEncoding.base64Url().omitPadding().encode(keyPair.getPublic().getEncoded()));
		var url = sb.toString();
		application.getHostServices().showDocument(url);
	}

	/* Getter/Setter */

	public String getHubUriHost() {
		var hubUri = hubUriRef.get();
		if (hubUri == null) {
			return null;
		} else {
			return hubUri.getHost();
		}
	}

	public BooleanBinding readyProperty() {
		return ready;
	}

	public boolean isReady() {
		return ready.get();
	}
}

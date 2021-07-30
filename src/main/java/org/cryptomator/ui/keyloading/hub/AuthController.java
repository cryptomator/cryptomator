package org.cryptomator.ui.keyloading.hub;

import com.google.common.base.Preconditions;
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
	private final ObjectBinding<URI> authUri;
	private final StringBinding authUriHost;
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
		this.authUri = Bindings.createObjectBinding(this::getAuthUri, redirectUriRef);
		this.authUriHost = Bindings.createStringBinding(this::getAuthUriHost, authUri);
		this.ready = authUri.isNotNull();
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
		LOG.info("Cryptomator Hub login succeeded: {} encrypted with {}", authParams, keyPair.getPublic());
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
		assert getAuthUri() != null;
		application.getHostServices().showDocument(getAuthUri().toString());
	}

	/* Getter/Setter */

	public ObjectBinding<URI> authUriProperty() {
		return authUri;
	}

	public URI getAuthUri() {
		var hubUri = hubUriRef.get();
		var redirectUri = redirectUriRef.get();
		if (hubUri == null || redirectUri == null) {
			return null;
		}
		var redirectParam = "redirect_uri=" + URLEncoder.encode(redirectUri.toString(), StandardCharsets.US_ASCII);
		try {
			return new URI(hubUri.getScheme(), hubUri.getAuthority(), hubUri.getPath(), redirectParam, null);
		} catch (URISyntaxException e) {
			throw new IllegalStateException("URI constructed from params known to be valid", e);
		}
	}

	public StringBinding authUriHostProperty() {
		return authUriHost;
	}

	public String getAuthUriHost() {
		var authUri = getAuthUri();
		if (authUri == null) {
			return null;
		} else {
			return authUri.getHost();
		}
	}

	public BooleanBinding readyProperty() {
		return ready;
	}

	public boolean isReady() {
		return ready.get();
	}
}

package org.cryptomator.ui.keyloading.hub;

import dagger.Lazy;
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
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class AuthFlowController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(AuthFlowController.class);

	private final Application application;
	private final Stage window;
	private final ExecutorService executor;
	private final String deviceId;
	private final HubConfig hubConfig;
	private final AtomicReference<String> tokenRef;
	private final UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result;
	private final Lazy<Scene> receiveKeyScene;
	private final ErrorComponent.Builder errorComponent;
	private final ObjectProperty<URI> authUri;
	private final StringBinding authHost;
	private AuthFlowTask task;

	@Inject
	public AuthFlowController(Application application, @KeyLoading Stage window, ExecutorService executor, @Named("deviceId") String deviceId, HubConfig hubConfig, @Named("bearerToken") AtomicReference<String> tokenRef, UserInteractionLock<HubKeyLoadingModule.HubLoadingResult> result, @FxmlScene(FxmlFile.HUB_RECEIVE_KEY) Lazy<Scene> receiveKeyScene, ErrorComponent.Builder errorComponent) {
		this.application = application;
		this.window = window;
		this.executor = executor;
		this.deviceId = deviceId;
		this.hubConfig = hubConfig;
		this.tokenRef = tokenRef;
		this.result = result;
		this.receiveKeyScene = receiveKeyScene;
		this.errorComponent = errorComponent;
		this.authUri = new SimpleObjectProperty<>();
		this.authHost = Bindings.createStringBinding(this::getAuthHost, authUri);
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void initialize() {
		assert task == null;
		task = new AuthFlowTask(hubConfig, new AuthFlowContext(deviceId), this::setAuthUri);;
		task.setOnFailed(this::authFailed);
		task.setOnSucceeded(this::authSucceeded);
		executor.submit(task);
	}

	@FXML
	public void browse() {
		application.getHostServices().showDocument(authUri.get().toString());
	}

	@FXML
	public void cancel() {
		window.close();
	}

	private void setAuthUri(URI uri) {
		authUri.set(uri);
		browse();
	}

	private void windowClosed(WindowEvent windowEvent) {
		// stop server, if it is still running
		task.cancel();
		// if not already interacted, mark this workflow as cancelled:
		if (result.awaitingInteraction().get()) {
			LOG.debug("Authorization cancelled by user.");
			result.interacted(HubKeyLoadingModule.HubLoadingResult.CANCELLED);
		}
	}

	private void authSucceeded(WorkerStateEvent workerStateEvent) {
		tokenRef.set(task.getValue());
		window.requestFocus();
		window.setScene(receiveKeyScene.get());
	}

	private void authFailed(WorkerStateEvent workerStateEvent) {
		result.interacted(HubKeyLoadingModule.HubLoadingResult.FAILED);
		window.requestFocus();
		var exception = workerStateEvent.getSource().getException();
		LOG.error("Authentication failed", exception);
		errorComponent.cause(exception).window(window).build().showErrorScene();
	}

	/* Getter/Setter */

	public StringBinding authHostProperty() {
		return authHost;
	}

	public String getAuthHost() {
		var uri = authUri.get();
		if (uri == null) {
			return "";
		} else {
			return uri.getAuthority().toString();
		}
	}

}

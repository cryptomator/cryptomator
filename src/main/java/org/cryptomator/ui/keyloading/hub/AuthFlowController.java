package org.cryptomator.ui.keyloading.hub;

import com.nimbusds.jose.JWEObject;
import dagger.Lazy;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.keyloading.KeyLoading;
import org.cryptomator.ui.keyloading.KeyLoadingScoped;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Application;
import javafx.application.Platform;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

@KeyLoadingScoped
public class AuthFlowController implements FxController {

	private final Application application;
	private final Stage window;
	private final ExecutorService executor;
	private final String deviceId;
	private final HubConfig hubConfig;
	private final AtomicReference<String> tokenRef;
	private final CompletableFuture<ReceivedKey> result;
	private final Lazy<Scene> receiveKeyScene;
	private final ObjectProperty<URI> authUri;
	private AuthFlowTask task;

	@Inject
	public AuthFlowController(Application application, @KeyLoading Stage window, ExecutorService executor, @Named("deviceId") String deviceId, HubConfig hubConfig, @Named("bearerToken") AtomicReference<String> tokenRef, CompletableFuture<ReceivedKey> result, @FxmlScene(FxmlFile.HUB_RECEIVE_KEY) Lazy<Scene> receiveKeyScene) {
		this.application = application;
		this.window = window;
		this.executor = executor;
		this.deviceId = deviceId;
		this.hubConfig = hubConfig;
		this.tokenRef = tokenRef;
		this.result = result;
		this.receiveKeyScene = receiveKeyScene;
		this.authUri = new SimpleObjectProperty<>();
		this.window.addEventHandler(WindowEvent.WINDOW_HIDING, this::windowClosed);
	}

	@FXML
	public void initialize() {
		assert task == null;
		task = new AuthFlowTask(hubConfig, new AuthFlowContext(deviceId), this::setAuthUri);
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
		Platform.runLater(() -> {
			authUri.set(uri);
			browse();
		});
	}

	private void windowClosed(WindowEvent windowEvent) {
		// stop server, if it is still running
		task.cancel();
		result.cancel(true);
	}

	private void authSucceeded(WorkerStateEvent workerStateEvent) {
		tokenRef.set(task.getValue());
		window.requestFocus();
		window.setScene(receiveKeyScene.get());
	}

	private void authFailed(WorkerStateEvent workerStateEvent) {
		window.requestFocus();
		var exception = workerStateEvent.getSource().getException();
		result.completeExceptionally(exception);
	}

}

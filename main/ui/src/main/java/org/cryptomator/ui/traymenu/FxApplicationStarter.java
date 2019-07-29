package org.cryptomator.ui.traymenu;

import javafx.application.Platform;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.fxapp.FxApplicationComponent;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@TrayMenuScoped
public class FxApplicationStarter {

	private final CompletableFuture<FxApplication> future;
	private final FxApplicationComponent.Builder fxAppComponent;

	@Inject
	public FxApplicationStarter(FxApplicationComponent.Builder fxAppComponent) {
		this.fxAppComponent = fxAppComponent;
		this.future = new CompletableFuture<>();
	}

	public synchronized FxApplication get() {
		if (!future.isDone()) {
			start();
		}
		try {
			return future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for FxApplication startup.", e);
		} catch (ExecutionException e) {
			throw new IllegalStateException("FxApplication startup failed.", e);
		}
	}

	private void start() {
		Platform.startup(() -> {
			assert Platform.isFxApplicationThread();
			FxApplication app = fxAppComponent.build().application();
			app.start();
			future.complete(app);
		});
	}

}

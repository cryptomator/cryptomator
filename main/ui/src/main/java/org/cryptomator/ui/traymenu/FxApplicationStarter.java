package org.cryptomator.ui.traymenu;

import javafx.application.Platform;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.fxapp.FxApplicationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@TrayMenuScoped
public class FxApplicationStarter {
	
	private static final Logger LOG = LoggerFactory.getLogger(FxApplicationStarter.class);

	private final CompletableFuture<FxApplication> future;
	private final FxApplicationComponent.Builder fxAppComponent;

	@Inject
	public FxApplicationStarter(FxApplicationComponent.Builder fxAppComponent) {
		this.fxAppComponent = fxAppComponent;
		this.future = new CompletableFuture<>();
	}

	public synchronized FxApplication get(boolean fromTrayMenu) {
		if (!future.isDone()) {
			start(fromTrayMenu);
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

	private void start(boolean fromTrayMenu) {
		LOG.debug("Starting JavaFX runtime...");
		Platform.startup(() -> {
			assert Platform.isFxApplicationThread();
			LOG.debug("JavaFX Runtime started.");
			FxApplication app = fxAppComponent.trayMenuSupported(fromTrayMenu).build().application();
			app.start();
			future.complete(app);
		});
	}

}

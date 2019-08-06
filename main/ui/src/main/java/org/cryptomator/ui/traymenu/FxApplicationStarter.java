package org.cryptomator.ui.traymenu;

import javafx.application.Platform;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.fxapp.FxApplicationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

@TrayMenuScoped
public class FxApplicationStarter {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplicationStarter.class);

	private final FxApplicationComponent.Builder fxAppComponent;
	private final ExecutorService executor;
	private final CompletableFuture<FxApplication> future;

	@Inject
	public FxApplicationStarter(FxApplicationComponent.Builder fxAppComponent, ExecutorService executor) {
		this.fxAppComponent = fxAppComponent;
		this.executor = executor;
		this.future = new CompletableFuture<>();
	}

	public synchronized CompletionStage<FxApplication> get(boolean fromTrayMenu) {
		if (!future.isDone()) {
			start(fromTrayMenu);
		}
		return future;
	}

	private void start(boolean fromTrayMenu) {
		executor.submit(() -> {
			LOG.debug("Starting JavaFX runtime...");
			Platform.startup(() -> {
				assert Platform.isFxApplicationThread();
				LOG.info("JavaFX Runtime started.");
				FxApplication app = fxAppComponent.trayMenuSupported(fromTrayMenu).build().application();
				app.start();
				future.complete(app);
			});
		});
	}

}

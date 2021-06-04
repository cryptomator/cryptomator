package org.cryptomator.ui.launcher;

import dagger.Lazy;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.fxapp.FxApplicationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class FxApplicationStarter {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplicationStarter.class);

	private final Lazy<FxApplicationComponent> fxAppComponent;
	private final ExecutorService executor;
	private final AtomicBoolean started;
	private final CompletableFuture<FxApplication> future;

	@Inject
	public FxApplicationStarter(Lazy<FxApplicationComponent> fxAppComponent, ExecutorService executor) {
		this.fxAppComponent = fxAppComponent;
		this.executor = executor;
		this.started = new AtomicBoolean();
		this.future = new CompletableFuture<>();
	}

	public CompletionStage<FxApplication> get() {
		if (!started.getAndSet(true)) {
			start();
		}
		return future;
	}

	private void start() {
		executor.submit(() -> {
			LOG.debug("Starting JavaFX runtime...");
			Platform.startup(() -> {
				assert Platform.isFxApplicationThread();
				LOG.info("JavaFX Runtime started.");
				FxApplication app = fxAppComponent.get().application();
				app.start();
				future.complete(app);
			});
		});
	}
}

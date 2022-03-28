package org.cryptomator.ui.launcher;

import com.google.common.base.Preconditions;
import org.cryptomator.ui.fxapp.FxApplication;
import org.cryptomator.ui.fxapp.FxApplicationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class FxApplicationStarter {

	private static final Logger LOG = LoggerFactory.getLogger(FxApplicationStarter.class);
	private static final AtomicReference<FxApplicationComponent.Builder> FX_APP_COMP_BUILDER = new AtomicReference<>();
	private static final CompletableFuture<FxApplication> FUTURE = new CompletableFuture<>();

	private final ExecutorService executor;
	private final AtomicBoolean started;

	@Inject
	public FxApplicationStarter(FxApplicationComponent.Builder fxAppCompBuilder, ExecutorService executor) {
		FX_APP_COMP_BUILDER.set(fxAppCompBuilder);
		this.executor = executor;
		this.started = new AtomicBoolean();
	}

	public CompletionStage<FxApplication> get() {
		if (!started.getAndSet(true)) {
			start();
		}
		return FUTURE;
	}

	private void start() {
		executor.submit(() -> {
			LOG.debug("Starting JavaFX runtime...");
			Application.launch(CryptomatorGui.class);
		});
	}

	public static class CryptomatorGui extends Application {

		@Override
		public void start(Stage primaryStage) throws Exception {
			var builder = Objects.requireNonNull(FX_APP_COMP_BUILDER.get()); // TODO add message?

			// set defaults for primary stage:
			// TODO: invoke StageFactory stuff...
			primaryStage.setTitle("Cryptomator");
			primaryStage.initStyle(StageStyle.UNDECORATED);
			primaryStage.setMinWidth(650);
			primaryStage.setMinHeight(440);

			// build subcomponent
			var comp = builder.mainWindow(primaryStage).fxApplication(this).build();

			// call delegate
			var app = comp.application();
			app.start();
			FUTURE.complete(app);
		}
	}
}

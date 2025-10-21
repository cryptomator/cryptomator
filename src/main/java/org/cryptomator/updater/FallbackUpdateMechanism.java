package org.cryptomator.updater;

import org.cryptomator.common.Environment;
import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.update.UpdateMechanism;
import org.cryptomator.integrations.update.UpdateStep;
import org.cryptomator.integrations.update.UpdateStepAdapter;
import org.cryptomator.ui.fxapp.FxApplicationScoped;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.application.Platform;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@FxApplicationScoped
@Priority(Priority.FALLBACK)
@DisplayName("Show Download Page") // TODO localize
public class FallbackUpdateMechanism implements UpdateMechanism {

	private static final String DOWNLOADS_URI_TEMPLATE = "https://cryptomator.org/downloads/" //
			+ "?utm_source=cryptomator-desktop" //
			+ "&utm_medium=update-notification&" //
			+ "utm_campaign=app-update-%s";

	private final Application app;
	private final Environment env;

	@Inject
	public FallbackUpdateMechanism(Application app, Environment env) {
		this.app = app;
		this.env = env;
	}

	@Override
	public boolean isUpdateAvailable(String currentVersion) {
		// FIXME: what source shall we use? self-hosted JSON?
		return true;
	}

	@Override
	public UpdateStep firstStep() {
		return UpdateStep.of("Go to download page", this::openDownloadPage); // TODO localize
	}

	private UpdateStep openDownloadPage() {
		var downloadUrl = DOWNLOADS_URI_TEMPLATE.formatted(URLEncoder.encode(env.getAppVersion(), StandardCharsets.US_ASCII));
		Platform.runLater(() -> {
			app.getHostServices().showDocument(downloadUrl);
		});
		return UpdateStep.RETRY; // allow running this "update mechanism" as many times as the user wants
	}

}

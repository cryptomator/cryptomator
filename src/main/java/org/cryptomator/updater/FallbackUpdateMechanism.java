package org.cryptomator.updater;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.integrations.common.DisplayName;
import org.cryptomator.integrations.common.Priority;
import org.cryptomator.integrations.update.UpdateInfo;
import org.cryptomator.integrations.update.UpdateMechanism;
import org.cryptomator.integrations.update.UpdateStep;
import org.cryptomator.integrations.update.UpdateStepAdapter;
import org.cryptomator.ui.fxapp.FxApplicationScoped;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.application.Platform;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@FxApplicationScoped
@Priority(Priority.FALLBACK)
@DisplayName("Show Download Page") // TODO localize
public class FallbackUpdateMechanism implements UpdateMechanism<UpdateInfo> {

	private static final Logger LOG = LoggerFactory.getLogger(FallbackUpdateMechanism.class);
	private static final String LATEST_VERSION_API_URL = "https://api.cryptomator.org/connect/apps/desktop/latest-version";
	private static final ObjectMapper MAPPER = new ObjectMapper();
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
	public UpdateInfo checkForUpdate(String currentVersion, HttpClient httpClient) {
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(LATEST_VERSION_API_URL)).build();
			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				throw new RuntimeException("Failed to fetch release: " + response.statusCode());
			}
			var release = MAPPER.readValue(response.body(), LatestVersion.class);
			var updateVersion = release.versionForCurrentOS();
			if (UpdateMechanism.isUpdateAvailable(updateVersion, currentVersion)) {
				return UpdateInfo.of(updateVersion, this);
			} else {
				return null;
			}
		} catch (IOException | InterruptedException e) {
			LOG.warn("Update check failed", e);
			return null;
		}
	}

	@Override
	public UpdateStep firstStep(UpdateInfo updateInfo) {
		return UpdateStep.of("Go to download page", this::openDownloadPage); // TODO localize
	}

	private UpdateStep openDownloadPage() {
		var downloadUrl = DOWNLOADS_URI_TEMPLATE.formatted(URLEncoder.encode(env.getAppVersion(), StandardCharsets.US_ASCII));
		Platform.runLater(() -> {
			app.getHostServices().showDocument(downloadUrl);
		});
		return UpdateStep.RETRY; // allow running this "update mechanism" as many times as the user wants
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LatestVersion(
			@JsonProperty("mac") String macVersion,
			@JsonProperty("win") String winVersion,
			@JsonProperty("linux") String linuxVersion
	) {
		public String versionForCurrentOS() {
			if (SystemUtils.IS_OS_MAC_OSX) {
				return macVersion;
			} else if (SystemUtils.IS_OS_WINDOWS) {
				return winVersion;
			} else if (SystemUtils.IS_OS_LINUX) {
				return linuxVersion;
			} else {
				throw new IllegalStateException("Unsupported operating system");
			}
		}
	}

}

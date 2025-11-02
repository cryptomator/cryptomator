package org.cryptomator.updater;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cryptomator.integrations.update.UpdateInfo;
import org.cryptomator.integrations.update.UpdateMechanism;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public abstract class DownloadUpdateMechanism implements UpdateMechanism {

	private static final Logger LOG = LoggerFactory .getLogger(DownloadUpdateMechanism.class);
	private static final String LATEST_VERSION_API_URL = "https://api.cryptomator.org/connect/apps/desktop/latest-version?format=1";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public UpdateInfo checkForUpdate(String currentVersion, HttpClient httpClient) {
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(LATEST_VERSION_API_URL)).build();
			HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) {
				throw new RuntimeException("Failed to fetch release: " + response.statusCode());
			}
			var release = MAPPER.readValue(response.body(), LatestVersionResponse.class);
			return checkForUpdate(currentVersion, release);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.debug("Update check interrupted.");
			return null;
		} catch (IOException e) {
			LOG.warn("Update check failed", e);
			return null;
		}
	}

	@Nullable
	@Blocking
	abstract UpdateInfo checkForUpdate(String currentVersion, LatestVersionResponse response);

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LatestVersionResponse(
			@JsonProperty("latestVersion") LatestVersion latestVersion,
			@JsonProperty("assets") List<Asset> assets
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LatestVersion(
			@JsonProperty("mac") String macVersion,
			@JsonProperty("win") String winVersion,
			@JsonProperty("linux") String linuxVersion
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Asset(
			@JsonProperty("name") String name,
			@JsonProperty("digest") String digest,
			@JsonProperty("size") long size,
			@JsonProperty("downloadUrl") String downloadUrl
	) {}

}

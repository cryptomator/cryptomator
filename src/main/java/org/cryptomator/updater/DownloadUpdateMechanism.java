package org.cryptomator.updater;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public abstract class DownloadUpdateMechanism implements UpdateMechanism {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public boolean isUpdateAvailable() {
		try (var client = HttpClient.newHttpClient()) {
			// TODO: check different source
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.github.com/repos/cryptomator/cryptomator/releases/latest")).header("Accept", "application/vnd.github+json").build();

			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

			if (response.statusCode() != 200) {
				throw new RuntimeException("Failed to fetch release: " + response.statusCode());
			}

			var release = MAPPER.readValue(response.body(), GitHubRelease.class);

			return release.assets.stream().anyMatch(a -> a.name.endsWith("arm64.dmg"));
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GitHubRelease(
			@JsonProperty("tag_name") String tagName,
			List<Asset> assets
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Asset(
			String name,
			@JsonProperty("browser_download_url") String downloadUrl
	) {}

}

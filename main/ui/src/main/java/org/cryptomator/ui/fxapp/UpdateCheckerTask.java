package org.cryptomator.ui.fxapp;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UpdateCheckerTask extends Task<String> {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateCheckerTask.class);

	private static final long MAX_RESPONSE_SIZE = 10 * 1024; // 10kb should be sufficient. protect against flooding
	private static final Gson GSON = new GsonBuilder().setLenient().create();

	private final HttpClient httpClient;
	private final HttpRequest checkForUpdatesRequest;

	UpdateCheckerTask(HttpClient httpClient, HttpRequest checkForUpdatesRequest) {
		this.httpClient = httpClient;
		this.checkForUpdatesRequest = checkForUpdatesRequest;

		setOnFailed(event -> LOG.error("Failed to check for updates", getException()));
	}

	@Override
	protected String call() throws IOException, InterruptedException {
		HttpResponse<InputStream> response = httpClient.send(checkForUpdatesRequest, HttpResponse.BodyHandlers.ofInputStream());
		if (response.statusCode() == 200) {
			return processBody(response);
		} else {
			throw new IOException("Unexpected HTTP response code " + response.statusCode());
		}
	}

	private String processBody(HttpResponse<InputStream> response) throws IOException {
		try (InputStream in = response.body(); //
			 InputStream limitedIn = ByteStreams.limit(in, MAX_RESPONSE_SIZE); //
			 Reader reader = new InputStreamReader(limitedIn, StandardCharsets.UTF_8)) {
			Map<String, String> map = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {
			}.getType());
			if (SystemUtils.IS_OS_MAC_OSX) {
				return map.get("mac");
			} else if (SystemUtils.IS_OS_WINDOWS) {
				return map.get("win");
			} else if (SystemUtils.IS_OS_LINUX) {
				return map.get("linux");
			} else {
				throw new IllegalStateException("Unsupported operating system");
			}
		}
	}
}

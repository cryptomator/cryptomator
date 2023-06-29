package org.cryptomator.ui.fxapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.concurrent.Task;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UpdateCheckerTask extends Task<String> {

	private static final ObjectMapper JSON = new ObjectMapper();
	private static final Logger LOG = LoggerFactory.getLogger(UpdateCheckerTask.class);

	private static final long MAX_RESPONSE_SIZE = 10L * 1024; // 10kb should be sufficient. protect against flooding

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
			 InputStream limitedIn = ByteStreams.limit(in, MAX_RESPONSE_SIZE)) {
			var json = JSON.reader().readTree(limitedIn);
			if (SystemUtils.IS_OS_MAC_OSX) {
				return json.get("mac").asText();
			} else if (SystemUtils.IS_OS_WINDOWS) {
				return json.get("win").asText();
			} else if (SystemUtils.IS_OS_LINUX) {
				return json.get("linux").asText();
			} else {
				throw new IllegalStateException("Unsupported operating system");
			}
		}
	}
}

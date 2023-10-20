package org.cryptomator.ui.fxapp;

import dagger.Module;
import dagger.Provides;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Module
public abstract class UpdateCheckerModule {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateCheckerModule.class);

	private static final URI LATEST_VERSION_URI = URI.create("https://api.cryptomator.org/desktop/latest-version.json");
	private static final Duration UPDATE_CHECK_INTERVAL = Duration.hours(3);
	private static final Duration DISABLED_UPDATE_CHECK_INTERVAL = Duration.hours(100000); // Duration.INDEFINITE leads to overflows...

	@Provides
	@Named("latestVersion")
	@FxApplicationScoped
	static StringProperty provideLatestVersion() {
		return new SimpleStringProperty();
	}

	@Provides
	@FxApplicationScoped
	static Optional<HttpClient> provideHttpClient() {
		try {
			return Optional.of(HttpClient.newBuilder() //
					.followRedirects(HttpClient.Redirect.NORMAL) // from version 1.6.11 onwards, Cryptomator can follow redirects, in case this URL ever changes
					.build());
		} catch (UncheckedIOException e) {
			LOG.error("HttpClient for update check cannot be created.", e);
			return Optional.empty();
		}
	}

	@Provides
	@FxApplicationScoped
	static HttpRequest provideCheckForUpdatesRequest(Environment env) {
		String userAgent = String.format("Cryptomator VersionChecker/%s %s %s (%s)", //
				env.getAppVersion(), //
				SystemUtils.OS_NAME, //
				SystemUtils.OS_VERSION, //
				SystemUtils.OS_ARCH); //
		return HttpRequest.newBuilder() //
				.uri(LATEST_VERSION_URI) //
				.header("User-Agent", userAgent) //
				.timeout(java.time.Duration.ofSeconds(10))
				.build();
	}

	@Provides
	@Named("checkForUpdatesInterval")
	@FxApplicationScoped
	static ObjectBinding<Duration> provideCheckForUpdateInterval(Settings settings) {
		return Bindings.when(settings.checkForUpdates).then(UPDATE_CHECK_INTERVAL).otherwise(DISABLED_UPDATE_CHECK_INTERVAL);
	}

	@Provides
	@FxApplicationScoped
	static ScheduledService<String> provideCheckForUpdatesService(ExecutorService executor, Optional<HttpClient> httpClient, HttpRequest checkForUpdatesRequest, @Named("checkForUpdatesInterval") ObjectBinding<Duration> period) {
		ScheduledService<String> service = new ScheduledService<>() {
			@Override
			protected Task<String> createTask() {
				if (httpClient.isPresent()) {
					return new UpdateCheckerTask(httpClient.get(), checkForUpdatesRequest);
				} else {
					return new Task<>() {
						@Override
						protected String call() {
							throw new NullPointerException("No HttpClient present.");
						}
					};
				}
			}
		};
		service.setOnFailed(event -> LOG.error("Failed to execute update service", service.getException()));
		service.setExecutor(executor);
		service.periodProperty().bind(period);
		return service;
	}


}

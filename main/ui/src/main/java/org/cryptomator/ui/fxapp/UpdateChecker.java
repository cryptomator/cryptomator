package org.cryptomator.ui.fxapp;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.WorkerStateEvent;
import javafx.util.Duration;
import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Comparator;
import java.util.Optional;

@FxApplicationScoped
public class UpdateChecker {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);

	private final Settings settings;
	private final Optional<String> applicationVersion;
	private final StringProperty latestVersionProperty;
	private final Comparator<String> semVerComparator;
	private final ScheduledService<String> updateCheckerService;

	@Inject
	UpdateChecker(Settings settings, @Named("applicationVersion") Optional<String> applicationVersion, @Named("latestVersion") StringProperty latestVersionProperty, @Named("SemVer") Comparator<String> semVerComparator, ScheduledService<String> updateCheckerService) {
		this.settings = settings;
		this.applicationVersion = applicationVersion;
		this.latestVersionProperty = latestVersionProperty;
		this.semVerComparator = semVerComparator;
		this.updateCheckerService = updateCheckerService;
	}

	public void startCheckingForUpdates(Duration initialDelay) {
		updateCheckerService.setDelay(initialDelay);
		updateCheckerService.setOnSucceeded(this::checkSucceeded);
		updateCheckerService.setOnFailed(this::checkFailed);
		updateCheckerService.restart();
	}

	public ReadOnlyStringProperty latestVersionProperty() {
		return latestVersionProperty;
	}

	private void checkSucceeded(WorkerStateEvent event) {
		String currentVersion = applicationVersion.orElse(null);
		String latestVersion = updateCheckerService.getValue();
		LOG.info("Current version: {}, lastest version: {}", currentVersion, latestVersion);

		// TODO settings.lastVersionCheck = Instant.now()
		if (currentVersion != null && semVerComparator.compare(currentVersion, latestVersion) < 0) {
			// update is available!
			latestVersionProperty.set(latestVersion);
		} else {
			latestVersionProperty.set(null);
		}
	}

	private void checkFailed(WorkerStateEvent event) {
		LOG.warn("Error checking for updates", event.getSource().getException());
	}

}

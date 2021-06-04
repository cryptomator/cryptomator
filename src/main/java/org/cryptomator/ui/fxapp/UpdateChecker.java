package org.cryptomator.ui.fxapp;

import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.util.Duration;
import java.util.Comparator;
import java.util.Optional;

@FxApplicationScoped
public class UpdateChecker {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);
	private static final Duration AUTOCHECK_DELAY = Duration.seconds(5);

	private final Settings settings;
	private final Optional<String> applicationVersion;
	private final StringProperty currentVersionProperty;
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
		this.currentVersionProperty = new SimpleStringProperty(applicationVersion.orElse("SNAPSHOT"));
	}

	public void automaticallyCheckForUpdatesIfEnabled() {
		if (settings.checkForUpdates().get()) {
			startCheckingForUpdates(AUTOCHECK_DELAY);
		}
	}

	public void checkForUpdatesNow() {
		startCheckingForUpdates(Duration.ZERO);
	}

	private void startCheckingForUpdates(Duration initialDelay) {
		updateCheckerService.cancel();
		updateCheckerService.reset();
		updateCheckerService.setDelay(initialDelay);
		updateCheckerService.setOnRunning(this::checkStarted);
		updateCheckerService.setOnSucceeded(this::checkSucceeded);
		updateCheckerService.setOnFailed(this::checkFailed);
		updateCheckerService.start();
	}

	private void checkStarted(WorkerStateEvent event) {
		LOG.debug("Checking for updates...");
	}

	private void checkSucceeded(WorkerStateEvent event) {
		String currentVersion = applicationVersion.orElse(null);
		String latestVersion = updateCheckerService.getValue();
		LOG.info("Current version: {}, lastest version: {}", currentVersion, latestVersion);

		if (currentVersion == null || semVerComparator.compare(currentVersion, latestVersion) < 0) {
			// update is available
			latestVersionProperty.set(latestVersion);
		} else {
			latestVersionProperty.set(null);
		}
	}

	private void checkFailed(WorkerStateEvent event) {
		LOG.warn("Error checking for updates", event.getSource().getException());
	}

	/* Observable Properties */

	public BooleanBinding checkingForUpdatesProperty() {
		return updateCheckerService.stateProperty().isEqualTo(Worker.State.RUNNING);
	}

	public ReadOnlyStringProperty latestVersionProperty() {
		return latestVersionProperty;
	}

	public ReadOnlyStringProperty currentVersionProperty() {
		return currentVersionProperty;
	}

}

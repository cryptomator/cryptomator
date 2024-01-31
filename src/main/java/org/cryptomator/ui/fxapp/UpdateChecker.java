package org.cryptomator.ui.fxapp;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.util.Duration;
import java.util.Comparator;

@FxApplicationScoped
public class UpdateChecker {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);
	private static final Duration AUTOCHECK_DELAY = Duration.seconds(5);

	private final Environment env;
	private final Settings settings;
	private final StringProperty latestVersionProperty;
	private final BooleanProperty upToDate;
	private final Comparator<String> semVerComparator;
	private final ScheduledService<String> updateCheckerService;

	@Inject
	UpdateChecker(Settings settings, Environment env, //
				  @Named("latestVersion") StringProperty latestVersionProperty, //
				  @Named("upToDate") BooleanProperty upToDate, //
				  @Named("SemVer") Comparator<String> semVerComparator, //
				  ScheduledService<String> updateCheckerService) {
		this.env = env;
		this.settings = settings;
		this.latestVersionProperty = latestVersionProperty;
		this.upToDate = upToDate;
		this.semVerComparator = semVerComparator;
		this.updateCheckerService = updateCheckerService;
	}

	public void automaticallyCheckForUpdatesIfEnabled() {
		if (!env.disableUpdateCheck() && settings.checkForUpdates.get()) {
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
		String latestVersion = updateCheckerService.getValue();
		LOG.info("Current version: {}, lastest version: {}", getCurrentVersion(), latestVersion);

		if (semVerComparator.compare(getCurrentVersion(), latestVersion) < 0) {
			// update is available
			latestVersionProperty.set(latestVersion);
			upToDate.set(false);
		} else {
			latestVersionProperty.set(null);
			upToDate.set(true);
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

	public String getCurrentVersion() {
		return env.getAppVersion();
	}

	public ReadOnlyBooleanProperty upToDateProperty() {
		return upToDate;
	}
}

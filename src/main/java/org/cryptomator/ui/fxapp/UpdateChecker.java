package org.cryptomator.ui.fxapp;

import org.cryptomator.common.Environment;
import org.cryptomator.common.SemVerComparator;
import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.util.Duration;
import java.time.Instant;
import java.util.Comparator;

@FxApplicationScoped
public class UpdateChecker {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);
	private static final Duration AUTO_CHECK_DELAY = Duration.seconds(5);

	private final Environment env;
	private final Settings settings;
	private final StringProperty latestVersion = new SimpleStringProperty();
	private final ScheduledService<String> updateCheckerService;
	private final ObjectProperty<UpdateCheckState> state = new SimpleObjectProperty<>(UpdateCheckState.NOT_CHECKED);
	private final ObjectProperty<Instant> lastSuccessfulUpdateCheck = new SimpleObjectProperty<>();
	private final Comparator<String> versionComparator = new SemVerComparator();
	private final BooleanBinding updateAvailable;

	@Inject
	UpdateChecker(Settings settings, //
				  Environment env, //
				  ScheduledService<String> updateCheckerService) {
		this.env = env;
		this.settings = settings;
		this.updateCheckerService = updateCheckerService;
		this.latestVersion.bindBidirectional(settings.latestVersion);
		this.lastSuccessfulUpdateCheck.bindBidirectional(settings.lastSuccessfulUpdateCheck);

		this.updateAvailable = Bindings.createBooleanBinding(() -> {
			var latestVersion = this.latestVersion.get();
			return latestVersion != null && versionComparator.compare(getCurrentVersion(), latestVersion) < 0;
		}, latestVersion);
	}

	public void automaticallyCheckForUpdatesIfEnabled() {
		if (!env.disableUpdateCheck() && settings.checkForUpdates.get()) {
			startCheckingForUpdates(AUTO_CHECK_DELAY);
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
		state.set(UpdateCheckState.IS_CHECKING);
	}

	private void checkSucceeded(WorkerStateEvent event) {
		var latestVersionString = updateCheckerService.getValue();
		LOG.info("Current version: {}, latest version: {}", getCurrentVersion(), latestVersionString);
		lastSuccessfulUpdateCheck.set(Instant.now());
		latestVersion.set(latestVersionString);
		state.set(UpdateCheckState.CHECK_SUCCESSFUL);
	}

	private void checkFailed(WorkerStateEvent event) {
		state.set(UpdateCheckState.CHECK_FAILED);
		LOG.warn("Error checking for updates", event.getSource().getException());
	}

	public enum UpdateCheckState {
		NOT_CHECKED,
		IS_CHECKING,
		CHECK_SUCCESSFUL,
		CHECK_FAILED;
	}

	/* Observable Properties */
	public BooleanBinding checkingForUpdatesProperty() {
		return updateCheckerService.stateProperty().isEqualTo(Worker.State.RUNNING);
	}

	public ReadOnlyStringProperty latestVersionProperty() {
		return latestVersion;
	}

	public BooleanBinding updateAvailableProperty() {
		return updateAvailable;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable.get();
	}

	public ObjectProperty<Instant> lastSuccessfulUpdateCheckProperty() {
		return lastSuccessfulUpdateCheck;
	}

	public ObjectProperty<UpdateCheckState> updateCheckStateProperty() {
		return state;
	}

	public String getCurrentVersion() {
		return env.getAppVersion();
	}
}

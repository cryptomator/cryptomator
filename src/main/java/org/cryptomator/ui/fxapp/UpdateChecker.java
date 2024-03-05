package org.cryptomator.ui.fxapp;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
import java.time.LocalDateTime;

@FxApplicationScoped
public class UpdateChecker {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);
	private static final Duration AUTO_CHECK_DELAY = Duration.seconds(5);

	private final Environment env;
	private final Settings settings;
	private final StringProperty latestVersionProperty = new SimpleStringProperty();
	private final ScheduledService<String> updateCheckerService;
	private final ObjectProperty<UpdateCheckState> state = new SimpleObjectProperty<>(UpdateCheckState.NOT_CHECKED);
	private final ObjectProperty<LocalDateTime> updateCheckTimeProperty = new SimpleObjectProperty<>();

	@Inject
	UpdateChecker(Settings settings, //
				  Environment env, //
				  ScheduledService<String> updateCheckerService) {
		this.env = env;
		this.settings = settings;
		this.updateCheckerService = updateCheckerService;
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
		String latestVersion = updateCheckerService.getValue();
		LOG.info("Current version: {}, latest version: {}", getCurrentVersion(), latestVersion);
		updateCheckTimeProperty.set(LocalDateTime.now());
		latestVersionProperty.set(latestVersion);
		state.set(UpdateCheckState.CHECK_SUCCESSFUL);
	}

	private void checkFailed(WorkerStateEvent event) {
		LOG.warn("Error checking for updates", event.getSource().getException());
		state.set(UpdateCheckState.CHECK_FAILED);
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
		return latestVersionProperty;
	}

	public String getCurrentVersion() {
		return env.getAppVersion();
	}

	public ObjectProperty<LocalDateTime> updateCheckTimeProperty() {
		return updateCheckTimeProperty;
	}

	public ObjectProperty<UpdateCheckState> updateCheckStateProperty() {
		return state;
	}

}

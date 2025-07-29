package org.cryptomator.ui.fxapp;

import org.cryptomator.common.Environment;
import org.cryptomator.common.SemVerComparator;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.updates.AppUpdateChecker;
import org.cryptomator.integrations.common.DistributionChannel;
import org.cryptomator.integrations.update.Progress;
import org.cryptomator.integrations.update.ProgressListener;
import org.cryptomator.integrations.update.UpdateFailedException;
import org.cryptomator.ui.preferences.UpdatesPreferencesController;
import org.purejava.portal.rest.UpdateCheckerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.application.Platform;
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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

@FxApplicationScoped
public class UpdateChecker {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);
	private static final Duration AUTO_CHECK_DELAY = Duration.seconds(5);

	private final Environment env;
	private final Settings settings;
	private final StringProperty latestVersion = new SimpleStringProperty();
	private final StringProperty latestAppUpdaterVersion = new SimpleStringProperty();
	private final ScheduledService<String> updateCheckerService;
	private final ObjectProperty<UpdateCheckState> state = new SimpleObjectProperty<>(UpdateCheckState.NOT_CHECKED);
	private final ObjectProperty<Instant> lastSuccessfulUpdateCheck;
	private final Comparator<String> versionComparator = new SemVerComparator();
	private final BooleanBinding updateAvailable;
	private final BooleanBinding appUpdateAvailable;
	private final BooleanBinding checkFailed;
	private final AppUpdateChecker updateChecker;
	private final FxApplicationTerminator appTerminator;

	@Inject
	UpdateChecker(Settings settings, //
				  Environment env, //
				  ScheduledService<String> updateCheckerService, //
				  AppUpdateChecker updateChecker, //
				  FxApplicationTerminator appTerminator) {
		this.env = env;
		this.settings = settings;
		this.updateCheckerService = updateCheckerService;
		this.lastSuccessfulUpdateCheck = settings.lastSuccessfulUpdateCheck;
		this.updateAvailable = Bindings.createBooleanBinding(this::isUpdateAvailable, latestVersion);
		this.appUpdateAvailable = Bindings.createBooleanBinding(this::isAppUpdateAvailable, latestAppUpdaterVersion);
		this.checkFailed = Bindings.equal(UpdateCheckState.CHECK_FAILED, state);
		this.updateChecker = updateChecker;
		this.appTerminator = appTerminator;
	}

	public void automaticallyCheckForUpdatesIfEnabled() {
		if (!env.disableUpdateCheck() && settings.checkForUpdates.get()) {
			decideOnUpdateChecker();
		}
	}

	public void checkForUpdatesNow() {
		decideOnUpdateChecker();
	}

	private void decideOnUpdateChecker() {
		if (updateChecker.isUpdateServiceAvailable(env.getBuildNumber())) { // prefer AppUpdateChecker
			switch (env.getBuildNumber().get()) {
				case "flatpak-1" -> startCheckingWithFlatpakUpdater((UpdateCheckerTask) updateChecker.getUpdater(DistributionChannel.Value.LINUX_FLATPAK), Duration.ZERO);
				default -> LOG.error("Unexpected value 'buildNumber': {}", env.getBuildNumber().get());
			}
		} else { // fallback is the "redirect user to website" approach
			startCheckingForUpdates(Duration.ZERO);
		}
	}

	public void updateAppNow() throws UpdateFailedException {
		var service = updateChecker.getServiceForChannel(DistributionChannel.Value.LINUX_FLATPAK);
		service.triggerUpdate();
	}

	public void terminateFlatpakOnUpdateCompleted(Runnable onComplete, UpdatesPreferencesController controller) {
		var service = updateChecker.getServiceForChannel(DistributionChannel.Value.LINUX_FLATPAK);
		service.addProgressListener(new ProgressListener() {
			@Override
			public void onProgress(Progress progress) {
				LOG.debug("Update progess is at percentage: {} and has status: {}", progress.getProgress(), progress.getStatus());

				if (progress.getStatus() == 0 || progress.getStatus() == 2) {
					Platform.runLater(() -> controller.flatpakProgressProperty().set(progress.getProgress() / 100.0));
				}

				if (progress.getStatus() == 2 && progress.getProgress() == 100) {
					LOG.debug("Update successfully finished, restarting App now");
					service.removeProgressListener(this);
					if (onComplete != null) {
						Platform.runLater(onComplete);
					}
					service.spawnApp();
					appTerminator.terminate();
				}
			}
		});
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

	private void startCheckingWithFlatpakUpdater(UpdateCheckerTask service, Duration initialDelay) {
		service.cancel();
		service.reset();
		service.setDelay(convertFxToJavaTime(initialDelay));
		service.setOnRunning(this::checkStarted);
		service.setOnSucceeded(this::checkSucceeded);
		service.setOnFailed(this::checkFailed);
		service.start();
	}

	private java.time.Duration convertFxToJavaTime(javafx.util.Duration fxDuration) {
		double millis = fxDuration.toMillis();
		return java.time.Duration.of((long) millis, ChronoUnit.MILLIS);
	}

	private void checkStarted(WorkerStateEvent event) {
		LOG.debug("Checking for updates...");
		state.set(UpdateCheckState.IS_CHECKING);
	}

	private void checkStarted() {
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

	private void checkSucceeded(String version) {
		LOG.info("Current version: {}, latest version: {}", getCurrentVersion(), version);
		lastSuccessfulUpdateCheck.set(Instant.now());
		latestAppUpdaterVersion.set(version);
		state.set(UpdateCheckState.CHECK_SUCCESSFUL);
	}

	private void checkFailed(WorkerStateEvent event) {
		state.set(UpdateCheckState.CHECK_FAILED);
	}

	private void checkFailed(Throwable throwable) {
		state.set(UpdateCheckState.CHECK_FAILED);
	}

	public enum UpdateCheckState {
		NOT_CHECKED,
		IS_CHECKING,
		CHECK_SUCCESSFUL,
		CHECK_FAILED
	}

	/* Observable Properties */
	public BooleanBinding checkingForUpdatesProperty() {
		return updateCheckerService.stateProperty().isEqualTo(Worker.State.RUNNING);
	}

	public ReadOnlyStringProperty latestVersionProperty() {
		return latestVersion;
	}

	public ReadOnlyStringProperty latestAppUpdaterVersionProperty() {
		return latestAppUpdaterVersion;
	}

	public BooleanBinding updateAvailableProperty() {
		return updateAvailable;
	}

	public BooleanBinding appUpdateAvailableProperty() {
		return appUpdateAvailable;
	}

	public BooleanBinding checkFailedProperty() {
		return checkFailed;
	}

	public boolean isUpdateAvailable(StringProperty versionProperty) {
		String currentVersion = getCurrentVersion();
		String latestVersionString = versionProperty.get();

		if (currentVersion == null || latestVersionString == null) {
			return false;
		}
		return versionComparator.compare(currentVersion, latestVersionString) < 0;
	}

	public boolean isUpdateAvailable() {
		return isUpdateAvailable(latestVersion);
	}

	public boolean isAppUpdateAvailable() {
		return isUpdateAvailable(latestAppUpdaterVersion);
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

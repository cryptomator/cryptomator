package org.cryptomator.ui.fxapp;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.update.UpdateFailedException;
import org.cryptomator.integrations.update.UpdateInfo;
import org.cryptomator.integrations.update.UpdateMechanism;
import org.cryptomator.updater.FallbackUpdateMechanism;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.util.Duration;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.concurrent.Executors;

@FxApplicationScoped
public class UpdateChecker extends ScheduledService<UpdateInfo> {

	private static final Logger LOG = LoggerFactory.getLogger(UpdateChecker.class);
	private static final Duration AUTO_CHECK_DELAY = Duration.seconds(5);
	private static final Duration UPDATE_CHECK_INTERVAL = Duration.hours(3);
	private static final Duration DISABLED_UPDATE_CHECK_INTERVAL = Duration.hours(100000); // Duration.INDEFINITE leads to overflows...

	public enum UpdateCheckState {
		NOT_CHECKED,
		IS_CHECKING,
		CHECK_SUCCESSFUL,
		CHECK_FAILED
	}

	private final Environment env;
	private final Settings settings;
	private final ObjectProperty<Instant> lastSuccessfulUpdateCheck;
	private final StringExpression latestVersion = StringExpression.stringExpression(lastValueProperty().map(UpdateInfo::version));
	private final BooleanBinding updateAvailable = lastValueProperty().isNotNull();
	private final ObjectBinding<UpdateCheckState> updateState = Bindings.createObjectBinding(this::getUpdateCheckState, stateProperty());
	private final BooleanBinding checkFailed = Bindings.equal(UpdateCheckState.CHECK_FAILED, updateState);
	private final HttpClient httpClient;
	private final UpdateMechanism primaryUpdateMechanism;
	private final UpdateMechanism fallbackUpdateMechanism;

	@Inject
	UpdateChecker(Settings settings, //
				  Environment env,
				  FallbackUpdateMechanism fallbackUpdateMechanism,
				  UpdateCheckerHttpClient httpClient) {
		this.env = env;
		this.settings = settings;
		this.lastSuccessfulUpdateCheck = settings.lastSuccessfulUpdateCheck;
		this.httpClient = httpClient;
		this.primaryUpdateMechanism = UpdateMechanism.get().orElse(fallbackUpdateMechanism);
		this.fallbackUpdateMechanism = fallbackUpdateMechanism;

		setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		periodProperty().bind(Bindings.when(settings.checkForUpdates).then(UPDATE_CHECK_INTERVAL).otherwise(DISABLED_UPDATE_CHECK_INTERVAL));
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
		cancel();
		reset();
		setDelay(initialDelay);
		start();
	}

	@Override
	protected void succeeded() {
		var updateInfo = getValue();
		super.succeeded(); // this will nil the value property!
		if (updateInfo != null) {
			LOG.info("Current version: {}, latest version: {}", getCurrentVersion(), updateInfo.version());
			lastSuccessfulUpdateCheck.set(Instant.now());
		}
	}

	@Override
	protected Task<UpdateInfo> createTask() {
		return new UpdateCheckTask();
	}

	@Override
	protected void failed() {
		super.failed();
		LOG.error("Update check failed.", getException());
	}


	/* Observable Properties */

	public StringExpression latestVersionProperty() {
		return latestVersion;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable.get();
	}

	public BooleanBinding updateAvailableProperty() {
		return updateAvailable;
	}

	public BooleanBinding checkFailedProperty() {
		return checkFailed;
	}

	public ObjectProperty<Instant> lastSuccessfulUpdateCheckProperty() {
		return lastSuccessfulUpdateCheck;
	}

	public ObjectBinding<UpdateCheckState> updateCheckStateProperty() {
		return updateState;
	}

	private UpdateCheckState getUpdateCheckState() {
		return switch (getState()) {
			case READY -> UpdateCheckState.NOT_CHECKED;
			case SCHEDULED, RUNNING -> UpdateCheckState.IS_CHECKING;
			case SUCCEEDED -> UpdateCheckState.CHECK_SUCCESSFUL;
			case FAILED, CANCELLED -> UpdateCheckState.CHECK_FAILED;
		};
	}

	public String getCurrentVersion() {
		return env.getAppVersion();
	}

	private class UpdateCheckTask extends Task<UpdateInfo> {

		@Override
		protected UpdateInfo call() throws UpdateFailedException, InterruptedException {
			var result = primaryUpdateMechanism.checkForUpdate(env.getAppVersion(), httpClient);
			if (result == null && primaryUpdateMechanism != fallbackUpdateMechanism) {
				LOG.debug("Primary update mechanism did not find an update. Try fallback update mechanism...");
				result = fallbackUpdateMechanism.checkForUpdate(env.getAppVersion(), httpClient);
			}
			return result;
		}
	}

}

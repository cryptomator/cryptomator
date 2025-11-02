package org.cryptomator.ui.preferences;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.update.UpdateInfo;
import org.cryptomator.integrations.update.UpdateStep;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.cryptomator.updater.FallbackUpdateMechanism;
import org.cryptomator.updater.UpdateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.ResourceBundle;


@PreferencesScoped
public class UpdatesPreferencesController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(UpdatesPreferencesController.class);

	private final Application application;
	private final Environment environment;
	private final ResourceBundle resourceBundle;
	private final Settings settings;
	private final UpdateChecker updateChecker;
	private final ObjectBinding<ContentDisplay> updateButtonState;
	private final StringExpression latestVersion;
	private final ObservableValue<Instant> lastSuccessfulUpdateCheck;
	private final StringBinding lastUpdateCheckMessage;
	private final ObservableValue<String> timeDifferenceMessage;
	private final String currentVersion;
	private final BooleanBinding checkFailed;
	private final BooleanProperty upToDateLabelVisible = new SimpleBooleanProperty(false);
	private final DateTimeFormatter formatter;
	private final BooleanBinding upToDate;
	private final UpdateService updateService;
	private final StringBinding updateButtonTitle;

	private final ObjectBinding<Worker<?>> worker;
	private final BooleanExpression running;

	/* FXML */
	public CheckBox checkForUpdatesCheckbox;

	@Inject
	UpdatesPreferencesController(Application application, Environment environment, ResourceBundle resourceBundle, Settings settings, UpdateChecker updateChecker, FallbackUpdateMechanism fallbackUpdateMechanism) {
		this.application = application;
		this.environment = environment;
		this.resourceBundle = resourceBundle;
		this.settings = settings;
		this.updateChecker = updateChecker;

		this.latestVersion = updateChecker.latestVersionProperty();
		this.lastSuccessfulUpdateCheck = updateChecker.lastSuccessfulUpdateCheckProperty();
		this.timeDifferenceMessage = Bindings.createStringBinding(this::getTimeDifferenceMessage, lastSuccessfulUpdateCheck);
		this.lastUpdateCheckMessage = Bindings.createStringBinding(this::getLastUpdateCheckMessage, lastSuccessfulUpdateCheck);

		this.currentVersion = updateChecker.getCurrentVersion();
		this.formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
		this.upToDate = updateChecker.updateCheckStateProperty().isEqualTo(UpdateChecker.UpdateCheckState.CHECK_SUCCESSFUL).and(latestVersion.isEqualTo(currentVersion));
		this.checkFailed = updateChecker.checkFailedProperty();

		this.updateService = new UpdateService(updateChecker.lastValueProperty().map(UpdateInfo::updateMechanism));
		this.worker = Bindings.when(updateChecker.updateAvailableProperty()).<Worker<?>>then(updateService).otherwise(updateChecker);
		this.running = Bindings.createBooleanBinding(this::isRunning, updateService.stateProperty(), updateChecker.stateProperty());
		this.updateButtonTitle = Bindings.createStringBinding(this::getUpdateButtonTitle, worker, updateService.stateProperty(), updateService.messageProperty());
		this.updateButtonState = Bindings.createObjectBinding(this::getUpdateButtonState, updateChecker.stateProperty(), updateService.stateProperty());

		updateChecker.updateAvailableProperty().addListener((_, _, newVal) -> LOG.info("Update available: {}", newVal));

		updateService.setOnSucceeded(this::updateSucceeded);
		updateService.setOnFailed(this::updateFailed);
	}

	public void initialize() {
		checkForUpdatesCheckbox.selectedProperty().bindBidirectional(settings.checkForUpdates);
		upToDate.addListener((_, _, newVal) -> {
			if (newVal) {
				upToDateLabelVisible.set(true);
				PauseTransition delay = new PauseTransition(javafx.util.Duration.seconds(5));
				delay.setOnFinished(_ -> upToDateLabelVisible.set(false));
				delay.play();
			}
		});
	}

	@FXML
	public void showLogfileDirectory() {
		environment.getLogDir().ifPresent(logDirPath -> application.getHostServices().showDocument(logDirPath.toUri().toString()));
	}

	@FXML
	public void startWork() {
		if (worker.get().equals(updateChecker)) {
			updateChecker.checkForUpdatesNow();
		} else if (worker.get().equals(updateService)) {
			updateService.start();
		}
	}

	private void updateSucceeded(WorkerStateEvent workerStateEvent) {
		assert workerStateEvent.getSource() == updateService;
		var lastStep = updateService.getValue();
		if (lastStep == UpdateStep.EXIT) {
			LOG.info("Exiting app to update...");
			Platform.exit();
		} else if (lastStep == UpdateStep.RETRY) {
			updateService.reset();
		} else {
			LOG.info("Update succeeded.");
		}
	}

	private void updateFailed(WorkerStateEvent workerStateEvent) {
		assert workerStateEvent.getSource() == updateService;
		LOG.error("Update failed.", updateService.getException());
	}

	/* Observable Properties */

	public ObjectBinding<ContentDisplay> updateButtonStateProperty() {
		return updateButtonState;
	}

	public ContentDisplay getUpdateButtonState() {
		if (updateService.isRunning()) { // isRunning() covers RUNNING and SCHEDULED states
			return ContentDisplay.BOTTOM;
		} else if (updateChecker.getState() == Worker.State.RUNNING) {
			return ContentDisplay.LEFT;
		} else {
			return ContentDisplay.TEXT_ONLY;
		}
	}

	public StringExpression latestVersionProperty() {
		return latestVersion;
	}

	public String getLatestVersion() {
		return latestVersion.get();
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public StringBinding lastUpdateCheckMessageProperty() {
		return lastUpdateCheckMessage;
	}

	public String getLastUpdateCheckMessage() {
		Instant lastCheck = lastSuccessfulUpdateCheck.getValue();
		if (lastCheck != null && !lastCheck.equals(Settings.DEFAULT_TIMESTAMP)) {
			return formatter.format(LocalDateTime.ofInstant(lastCheck, ZoneId.systemDefault()));
		} else {
			return "-";
		}
	}

	public ObservableValue<String> timeDifferenceMessageProperty() {
		return timeDifferenceMessage;
	}

	public String getTimeDifferenceMessage() {
		var lastSuccessCheck = lastSuccessfulUpdateCheck.getValue();
		var duration = Duration.between(lastSuccessCheck, Instant.now());
		var hours = duration.toHours();
		if (lastSuccessCheck.equals(Settings.DEFAULT_TIMESTAMP)) {
			return resourceBundle.getString("preferences.updates.lastUpdateCheck.never");
		} else if (hours < 1) {
			return resourceBundle.getString("preferences.updates.lastUpdateCheck.recently");
		} else if (hours < 24) {
			return String.format(resourceBundle.getString("preferences.updates.lastUpdateCheck.hoursAgo"), hours);
		} else {
			return String.format(resourceBundle.getString("preferences.updates.lastUpdateCheck.daysAgo"), duration.toDays());
		}
	}

	public BooleanProperty upToDateLabelVisibleProperty() {
		return upToDateLabelVisible;
	}

	public boolean isUpToDateLabelVisible() {
		return upToDateLabelVisible.get();
	}

	public BooleanBinding checkFailedProperty() {
		return checkFailed;
	}

	public boolean isCheckFailed() {
		return checkFailed.getValue();
	}

	public ObjectBinding<Worker<?>> workerProperty() {
		return worker;
	}

	public Worker<?> getWorker() {
		return worker.get();
	}

	public BooleanExpression runningProperty() {
		return running;
	}

	public boolean isRunning() {
		return updateChecker.getState() == Worker.State.RUNNING || updateService.getState() == Worker.State.RUNNING;
	}

	public StringBinding updateButtonTitleProperty() {
		return updateButtonTitle;
	}

	public String getUpdateButtonTitle() {
		if (worker.get() == updateChecker) {
			return resourceBundle.getString("preferences.updates.checkNowBtn");
		} else {
			return switch (updateService.getState()) {
				case READY -> updateChecker.getLastValue().updateMechanism().getName();
				case SCHEDULED, RUNNING -> updateService.getMessage(); // "Preparing Update..."; // TODO: resourceBundle.getString("preferences.updates.preparingUpdate")...
				case SUCCEEDED -> "Restart to Update"; // TODO: resourceBundle.getString("preferences.updates.readyToRestart")...
				case FAILED, CANCELLED -> "failed";
			};
		}
	}

	public UpdateChecker getUpdateChecker() {
		return updateChecker;
	}

}

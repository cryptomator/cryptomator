package org.cryptomator.ui.preferences;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.integrations.update.UpdateStep;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.VaultService;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.cryptomator.updater.FallbackUpdateInfo;
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());

	private final Application application;
	private final Environment environment;
	private final ResourceBundle resourceBundle;
	private final Settings settings;
	private final UpdateChecker updateChecker;
	private final UpdateService updateService;
	private final ObservableList<Vault> unlockedVaults;
	private final VaultService vaultService;
	private final ObjectBinding<Worker<?>> worker;
	private final BooleanExpression running;
	private final StringBinding updateButtonTitle;
	private final ObjectBinding<ContentDisplay> updateButtonState;
	private final ObservableValue<String> timeDifferenceMessage;
	private final StringBinding lastUpdateCheckMessage;

	private final BooleanBinding prohibitUpdateWhileUnlocked;
	private final BooleanBinding updateButtonDisabled;
	private final BooleanProperty upToDateLabelVisible = new SimpleBooleanProperty(false);

	/* FXML */
	public CheckBox checkForUpdatesCheckbox;

	@Inject
	UpdatesPreferencesController(Application application, Environment environment, ResourceBundle resourceBundle, Settings settings, UpdateChecker updateChecker, ObservableList<Vault> vaults, VaultService vaultService) {
		this.application = application;
		this.environment = environment;
		this.resourceBundle = resourceBundle;
		this.settings = settings;
		this.updateChecker = updateChecker;
		this.updateService = new UpdateService(updateChecker.lastValueProperty());
		this.vaultService = vaultService;
		this.worker = Bindings.when(updateChecker.updateAvailableProperty()).<Worker<?>>then(this.updateService).otherwise(this.updateChecker);
		this.running = Bindings.createBooleanBinding(this::isRunning, updateService.stateProperty(), updateChecker.stateProperty());
		this.updateButtonTitle = Bindings.createStringBinding(this::getUpdateButtonTitle, worker, updateService.stateProperty(), updateService.messageProperty());
		this.updateButtonState = Bindings.createObjectBinding(this::getUpdateButtonState, updateChecker.stateProperty(), updateService.stateProperty());
		this.timeDifferenceMessage = Bindings.createStringBinding(this::getTimeDifferenceMessage, updateChecker.lastSuccessfulUpdateCheckProperty());
		this.lastUpdateCheckMessage = Bindings.createStringBinding(this::getLastUpdateCheckMessage, updateChecker.lastSuccessfulUpdateCheckProperty());
		this.unlockedVaults = vaults.filtered(Vault::isUnlocked);
		this.prohibitUpdateWhileUnlocked = Bindings.createBooleanBinding(this::isProhibitUpdateWhileUnlocked, unlockedVaults, updateChecker.lastValueProperty());
		this.updateButtonDisabled = Bindings.when(worker.isEqualTo(updateChecker)).then(running).otherwise(prohibitUpdateWhileUnlocked.or(running));
	}

	public void initialize() {
		checkForUpdatesCheckbox.selectedProperty().bindBidirectional(settings.checkForUpdates);
		updateChecker.updateAvailableProperty().addListener((_, _, hasUpdate) -> {
			if (!hasUpdate) {
				upToDateLabelVisible.set(true);
				PauseTransition delay = new PauseTransition(javafx.util.Duration.seconds(5));
				delay.setOnFinished(_ -> upToDateLabelVisible.set(false));
				delay.play();
			}
		});
		updateService.setOnSucceeded(this::updateSucceeded);
		updateService.setOnFailed(this::updateFailed);
	}

	@FXML
	public void showLogfileDirectory() {
		environment.getLogDir().ifPresent(logDirPath -> application.getHostServices().showDocument(logDirPath.toUri().toString()));
	}

	@FXML
	public void startWork() {
		if (worker.get().equals(updateChecker)) {
			updateChecker.checkForUpdatesNow();
		} else if (!unlockedVaults.isEmpty()) {
			LOG.warn("Cannot start update due to unlocked vaults.");
		} else if (worker.get().equals(updateService)) {
			LOG.info("User started update to version {}", updateChecker.getLastValue().version());
			updateService.start();
		}
	}

	private void updateSucceeded(WorkerStateEvent workerStateEvent) {
		assert workerStateEvent.getSource() == updateService;
		var lastStep = updateService.getValue();
		if (lastStep == UpdateStep.EXIT) {
			// Record that this version attempted an update, so next launch can choose fallback if needed
			settings.lastUpdateAttemptedByVersion.set(environment.getAppVersionWithBuildNumber());
			settings.saveNow();
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
		// TODO: show error to user? use fallback update service?
		updateService.reset();
	}

	@FXML
	public void lockAllGracefully() {
		vaultService.lockAll(unlockedVaults, false);
	}

	/* Observable Properties */

	public UpdateChecker getUpdateChecker() {
		return updateChecker;
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

	public ObservableValue<String> timeDifferenceMessageProperty() {
		return timeDifferenceMessage;
	}

	public String getTimeDifferenceMessage() {
		var lastSuccessCheck = updateChecker.getLastSuccessfulUpdateCheck();
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

	public StringBinding lastUpdateCheckMessageProperty() {
		return lastUpdateCheckMessage;
	}

	public String getLastUpdateCheckMessage() {
		Instant lastCheck = updateChecker.getLastSuccessfulUpdateCheck();
		if (lastCheck != null && !lastCheck.equals(Settings.DEFAULT_TIMESTAMP)) {
			return FORMATTER.format(LocalDateTime.ofInstant(lastCheck, ZoneId.systemDefault()));
		} else {
			return "-";
		}
	}

	public boolean isProhibitUpdateWhileUnlocked() {
		// If the result of the last update check was from the fallback mechanism, we don't need to show the warning
		return !unlockedVaults.isEmpty() && !FallbackUpdateInfo.class.isInstance(updateChecker.getLastValue());
	}

	public BooleanBinding prohibitUpdateWhileUnlockedProperty() {
		return prohibitUpdateWhileUnlocked;
	}

	public boolean isUpdateButtonDisabled() {
		return updateButtonDisabled.get();
	}

	public BooleanBinding updateButtonDisabledProperty() {
		return updateButtonDisabled;
	}

	public BooleanProperty upToDateLabelVisibleProperty() {
		return upToDateLabelVisible;
	}

	public boolean isUpToDateLabelVisible() {
		return upToDateLabelVisible.get();
	}

}

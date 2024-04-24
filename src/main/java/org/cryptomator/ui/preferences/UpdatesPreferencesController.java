package org.cryptomator.ui.preferences;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.UpdateChecker;

import javax.inject.Inject;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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

	private static final String DOWNLOADS_URI = "https://cryptomator.org/downloads";

	private final Application application;
	private final Environment environment;
	private final ResourceBundle resourceBundle;
	private final Settings settings;
	private final UpdateChecker updateChecker;
	private final ObjectBinding<ContentDisplay> checkForUpdatesButtonState;
	private final ReadOnlyStringProperty latestVersion;
	private final ObservableValue<Instant> lastSuccessfulUpdateCheck;
	private final ObservableValue<String> timeDifferenceMessage;
	private final String currentVersion;
	private final BooleanBinding updateAvailable;
	private final BooleanProperty upToDateLabelVisible = new SimpleBooleanProperty(false);
	private final ObjectProperty<UpdateChecker.UpdateCheckState> updateCheckState;
	private final DateTimeFormatter formatter;

	/* FXML */
	public CheckBox checkForUpdatesCheckbox;
	public HBox checkFailedHBox;
	public Label upToDateLabel;

	@Inject
	UpdatesPreferencesController(Application application, Environment environment, ResourceBundle resourceBundle, Settings settings, UpdateChecker updateChecker) {
		this.application = application;
		this.environment = environment;
		this.resourceBundle = resourceBundle;
		this.settings = settings;
		this.updateChecker = updateChecker;
		this.checkForUpdatesButtonState = Bindings.when(updateChecker.checkingForUpdatesProperty()).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
		this.latestVersion = updateChecker.latestVersionProperty();
		this.lastSuccessfulUpdateCheck = updateChecker.lastSuccessfulUpdateCheckProperty();
		this.timeDifferenceMessage = lastSuccessfulUpdateCheck.map(this::updateTimeDifferenceMessage);
		this.currentVersion = updateChecker.getCurrentVersion();
		this.updateAvailable = updateChecker.updateAvailableProperty();
		this.updateCheckState = updateChecker.updateCheckStateProperty();
		this.formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
	}

	public void initialize() {
		checkForUpdatesCheckbox.selectedProperty().bindBidirectional(settings.checkForUpdates);

		BooleanBinding isUpdateSuccessfulAndCurrent = updateCheckState.isEqualTo(UpdateChecker.UpdateCheckState.CHECK_SUCCESSFUL).and(latestVersion.isEqualTo(currentVersion));

		updateCheckState.addListener((_, _, _) -> {
			if (isUpdateSuccessfulAndCurrent.get()) {
				upToDateLabelVisible.set(true);
				PauseTransition delay = new PauseTransition(javafx.util.Duration.seconds(5));
				delay.setOnFinished(_ -> upToDateLabelVisible.set(false));
				delay.play();
			}
		});
	}

	@FXML
	public void checkNow() {
		updateChecker.checkForUpdatesNow();
	}

	@FXML
	public void visitDownloadsPage() {
		application.getHostServices().showDocument(DOWNLOADS_URI);
	}

	@FXML
	public void showLogfileDirectory() {
		environment.getLogDir().ifPresent(logDirPath -> application.getHostServices().showDocument(logDirPath.toUri().toString()));
	}

	/* Observable Properties */

	public ObjectBinding<ContentDisplay> checkForUpdatesButtonStateProperty() {
		return checkForUpdatesButtonState;
	}

	public ContentDisplay getCheckForUpdatesButtonState() {
		return checkForUpdatesButtonState.get();
	}

	public ReadOnlyStringProperty latestVersionProperty() {
		return latestVersion;
	}

	public String getLatestVersion() {
		return latestVersion.isNotNull().get() ? latestVersion.get() : "-";
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public ObservableValue<Instant> lastSuccessfulUpdateCheckProperty() {
		return lastSuccessfulUpdateCheck;
	}

	public String getLastSuccessfulUpdateCheck() {
		Instant lastCheck = lastSuccessfulUpdateCheck.getValue();
		if (lastCheck != null && !lastCheck.equals(Settings.DEFAULT_LAST_SUCCESSFUL_UPDATE_CHECK)) {
			return formatter.format(LocalDateTime.ofInstant(lastCheck, ZoneId.systemDefault()));
		} else {
			return "-";
		}
	}

	private String updateTimeDifferenceMessage(Instant lastSuccessCheck) {
		if (lastSuccessCheck.equals(Settings.DEFAULT_LAST_SUCCESSFUL_UPDATE_CHECK)) {
			return resourceBundle.getString("preferences.updates.lastUpdateCheck.never");
		}

		var duration = Duration.between(lastSuccessCheck, Instant.now());
		var hours = duration.toHours();
		if (hours < 1) {
			return resourceBundle.getString("preferences.updates.lastUpdateCheck.recently");
		} else if (hours < 24) {
			return String.format(resourceBundle.getString("preferences.updates.lastUpdateCheck.hoursAgo"), hours);
		} else {
			return String.format(resourceBundle.getString("preferences.updates.lastUpdateCheck.daysAgo"), duration.toDays());
		}
	}


	public ObservableValue<String> timeDifferenceMessageProperty() {
		return timeDifferenceMessage;
	}

	public String getTimeDifferenceMessage() {
		return timeDifferenceMessage.getValue();
	}

	public BooleanProperty upToDateLabelVisibleProperty() {
		return upToDateLabelVisible;
	}

	public boolean isUpToDateLabelVisible() {
		return upToDateLabelVisible.get();
	}

	public BooleanBinding updateAvailableProperty() {
		return updateAvailable;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable.get();
	}

	public BooleanBinding checkFailedProperty() {
		return updateCheckState.isEqualTo(UpdateChecker.UpdateCheckState.CHECK_FAILED);
	}

	public boolean isCheckFailed() {
		return checkFailedProperty().get();
	}

}

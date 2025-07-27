package org.cryptomator.ui.preferences;

import org.cryptomator.common.Environment;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.updates.AppUpdateChecker;
import org.cryptomator.integrations.common.DistributionChannel;
import org.cryptomator.integrations.update.UpdateFailedException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.UpdateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ProgressBar;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
	private static final String DOWNLOADS_URI_TEMPLATE = "https://cryptomator.org/downloads/" //
			+ "?utm_source=cryptomator-desktop" //
			+ "&utm_medium=update-notification&" //
			+ "utm_campaign=app-update-%s";

	private final Application application;
	private final Environment environment;
	private final ResourceBundle resourceBundle;
	private final Settings settings;
	private final Environment env;
	private final UpdateChecker updateChecker;
	private final AppUpdateChecker appUpdateChecker;
	private final ObjectBinding<ContentDisplay> checkForUpdatesButtonState;
	private final ReadOnlyStringProperty latestVersion;
	private final ObservableValue<Instant> lastSuccessfulUpdateCheck;
	private final StringBinding lastUpdateCheckMessage;
	private final ObservableValue<String> timeDifferenceMessage;
	private final String currentVersion;
	private final BooleanBinding updateAvailable;
	private final BooleanBinding appUpdateAvailable;
	private final BooleanBinding checkFailed;
	private final BooleanProperty upToDateLabelVisible = new SimpleBooleanProperty(false);
	private final DateTimeFormatter formatter;
	private final BooleanBinding upToDate;
	private final String downloadsUri;
	private final BooleanProperty updatingFlatpak = new SimpleBooleanProperty(false);
	private final DoubleProperty flatpakProgress = new SimpleDoubleProperty(ProgressBar.INDETERMINATE_PROGRESS);

	/* FXML */
	public CheckBox checkForUpdatesCheckbox;
	@FXML
	public Button flatpakUpdateButton;

	@Inject
	UpdatesPreferencesController(Application application, Environment environment, ResourceBundle resourceBundle, Settings settings, UpdateChecker updateChecker, AppUpdateChecker appUpdateChecker, Environment env) {
		this.application = application;
		this.environment = environment;
		this.resourceBundle = resourceBundle;
		this.settings = settings;
		this.env = env;
		this.updateChecker = updateChecker;
		this.appUpdateChecker = appUpdateChecker;
		this.checkForUpdatesButtonState = Bindings.when(updateChecker.checkingForUpdatesProperty()).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
		this.latestVersion = updateChecker.latestVersionProperty();
		this.lastSuccessfulUpdateCheck = updateChecker.lastSuccessfulUpdateCheckProperty();
		this.timeDifferenceMessage = Bindings.createStringBinding(this::getTimeDifferenceMessage, lastSuccessfulUpdateCheck);
		this.currentVersion = environment.getAppVersion();
		this.updateAvailable = updateChecker.updateAvailableProperty();
		this.appUpdateAvailable = updateChecker.appUpdateAvailableProperty();
		this.formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
		this.upToDate = updateChecker.updateCheckStateProperty().isEqualTo(UpdateChecker.UpdateCheckState.CHECK_SUCCESSFUL).and(latestVersion.isEqualTo(currentVersion));
		this.checkFailed = updateChecker.checkFailedProperty();
		this.lastUpdateCheckMessage = Bindings.createStringBinding(this::getLastUpdateCheckMessage, lastSuccessfulUpdateCheck);
		this.downloadsUri = DOWNLOADS_URI_TEMPLATE.formatted(URLEncoder.encode(currentVersion, StandardCharsets.US_ASCII));
	}

	public void initialize() {
		checkForUpdatesCheckbox.selectedProperty().bindBidirectional(settings.checkForUpdates);
		switch (env.getBuildNumber().get()) {
			case "flatpak-1" -> flatpakUpdateButton.setText(appUpdateChecker.getServiceForChannel(DistributionChannel.Value.LINUX_FLATPAK).getDisplayName());
			default -> LOG.error("Unexpected value 'buildNumber': {}", env.getBuildNumber().get());
		}

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
	public void checkNow() {
		updateChecker.checkForUpdatesNow();
	}

	@FXML
	public void updateFlatpakNow() {
		updatingFlatpak.set(true);
		updateChecker.terminateFlatpakOnUpdateCompleted(
				() -> updatingFlatpak.set(false), this
		);

		try {
			updateChecker.updateAppNow();
		} catch (UpdateFailedException e) {
			updatingFlatpak.set(false);
		}
	}

	@FXML
	public void visitDownloadsPage() {
		application.getHostServices().showDocument(downloadsUri);
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

	public BooleanBinding updateAvailableProperty() {
		return updateAvailable;
	}

	public BooleanBinding appUdateAvailableProperty() {
		return appUpdateAvailable;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable.get();
	}

	public boolean isAppUpdateAvailable() {
		return appUpdateAvailable.get();
	}

	public BooleanBinding checkFailedProperty() {
		return checkFailed;
	}

	public boolean isCheckFailed() {
		return checkFailed.getValue();
	}

	public BooleanProperty updatingFlatpakProperty() {
		return updatingFlatpak;
	}

	public boolean isUpdatingFlatpak() {
		return updatingFlatpak.get();
	}

	public DoubleProperty flatpakProgressProperty() {
		return flatpakProgress;
	}
}

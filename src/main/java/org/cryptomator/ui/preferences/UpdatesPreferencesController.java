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
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;


@PreferencesScoped
public class UpdatesPreferencesController implements FxController {

	private static final String DOWNLOADS_URI = "https://cryptomator.org/downloads";

	private final Application application;
	private final Environment environment;
	private final Settings settings;
	private final UpdateChecker updateChecker;
	private final ObjectBinding<ContentDisplay> checkForUpdatesButtonState;
	private final ReadOnlyStringProperty latestVersion;
	private final ObjectProperty<LocalDateTime> updateCheckDate;
	private final ReadOnlyStringProperty timeDifferenceMessage;
	private final String currentVersion;
	private final BooleanBinding updateAvailable;
	private final BooleanProperty upToDateLabelVisible = new SimpleBooleanProperty(false);
	private final ObjectProperty<UpdateChecker.UpdateCheckState> updateCheckStateProperty;

	/* FXML */
	public CheckBox checkForUpdatesCheckbox;
	public HBox checkFailedHBox;
	public Label upToDateLabel;

	@Inject
	UpdatesPreferencesController(Application application, Environment environment, Settings settings, UpdateChecker updateChecker) {
		this.application = application;
		this.environment = environment;
		this.settings = settings;
		this.updateChecker = updateChecker;
		this.checkForUpdatesButtonState = Bindings.when(updateChecker.checkingForUpdatesProperty()).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
		this.latestVersion = updateChecker.latestVersionProperty();
		this.updateCheckDate = updateChecker.updateCheckTimeProperty();
		this.timeDifferenceMessage = updateChecker.timeDifferenceMessageProperty();
		this.currentVersion = updateChecker.getCurrentVersion();
		this.updateAvailable = updateChecker.updateAvailableProperty();
		this.updateCheckStateProperty = updateChecker.updateCheckStateProperty();
	}

	public void initialize() {
		checkForUpdatesCheckbox.selectedProperty().bindBidirectional(settings.checkForUpdates);

		BooleanBinding isUpdateSuccessfulAndCurrent = updateCheckStateProperty.isEqualTo(UpdateChecker.UpdateCheckState.CHECK_SUCCESSFUL).and(latestVersion.isEqualTo(currentVersion));

		updateCheckStateProperty.addListener((_, _, _) -> {
			if (isUpdateSuccessfulAndCurrent.get()) {
				upToDateLabelVisible.set(true);
				PauseTransition delay = new PauseTransition(Duration.seconds(5));
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

	public ObjectProperty<LocalDateTime> updateCheckDateProperty() {
		return updateCheckDate;
	}

	public String getUpdateCheckDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
		return !updateCheckDate.get().equals(LocalDateTime.parse(Settings.DEFAULT_LAST_SUCCESSFUL_UPDATE_CHECK)) ? updateCheckDate.get().format(formatter) : "-";
	}

	public ReadOnlyStringProperty timeDifferenceMessageProperty(){
		return timeDifferenceMessage;
	}

	public String getTimeDifferenceMessage() {
		return timeDifferenceMessage.get();
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
		return updateCheckStateProperty.isEqualTo(UpdateChecker.UpdateCheckState.CHECK_FAILED);
	}

	public boolean isCheckFailed() {
		return checkFailedProperty().get();
	}

}

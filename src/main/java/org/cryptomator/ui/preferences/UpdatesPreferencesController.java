package org.cryptomator.ui.preferences;

import org.cryptomator.common.SemVerComparator;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.controls.FormattedLabel;
import org.cryptomator.ui.fxapp.UpdateChecker;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;

@PreferencesScoped
public class UpdatesPreferencesController implements FxController {

	private static final String DOWNLOADS_URI = "https://cryptomator.org/downloads";

	private final Application application;
	private final Settings settings;
	private final ResourceBundle resourceBundle;
	private final UpdateChecker updateChecker;
	private final ObjectBinding<ContentDisplay> checkForUpdatesButtonState;
	private final ReadOnlyStringProperty latestVersion;
	private final String currentVersion;
	private final BooleanBinding updateAvailable;
	private final ObjectProperty<LocalDateTime> updateCheckDateProperty;
	private final Comparator<String> versionComparator = new SemVerComparator();
	private final ObjectProperty<UpdateChecker.UpdateCheckState> updateCheckStateProperty;

	/* FXML */
	public CheckBox checkForUpdatesCheckbox;
	public FormattedLabel updateCheckDateFormattedLabel;
	public FormattedLabel statusFormattedLabel;

	@Inject
	UpdatesPreferencesController(Application application, Settings settings, ResourceBundle resourceBundle, UpdateChecker updateChecker) {
		this.application = application;
		this.settings = settings;
		this.resourceBundle = resourceBundle;
		this.updateChecker = updateChecker;
		this.checkForUpdatesButtonState = Bindings.when(updateChecker.checkingForUpdatesProperty()).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
		this.latestVersion = updateChecker.latestVersionProperty();
		this.currentVersion = updateChecker.getCurrentVersion();
		this.updateAvailable = Bindings.createBooleanBinding(() -> {
			if (latestVersion.get() != null) {
				return versionComparator.compare(currentVersion, latestVersion.get()) < 0;
			} else {
				return false;
			}
		}, latestVersion);
		this.updateCheckDateProperty = updateChecker.updateCheckTimeProperty();
		this.updateCheckStateProperty = updateChecker.updateCheckStateProperty();
	}

	public void initialize() {
		checkForUpdatesCheckbox.selectedProperty().bindBidirectional(settings.checkForUpdates);

		DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
		updateCheckDateFormattedLabel.arg1Property().bind(Bindings.createStringBinding(() -> {
			 return (updateCheckDateProperty.get() != null) ? updateCheckDateProperty.get().format(formatter) : "";
		}, updateCheckDateProperty));
		updateCheckDateFormattedLabel.managedProperty().bind(updateCheckDateProperty.isNotNull());
		updateCheckDateFormattedLabel.visibleProperty().bind(updateCheckDateProperty.isNotNull());

		statusFormattedLabel.arg1Property().bind(Bindings.createObjectBinding(() ->{
					return switch (updateCheckStateProperty.get()) {
						case NOT_CHECKED -> resourceBundle.getString("preferences.updates.status.notChecked");
						case IS_CHECKING -> resourceBundle.getString("preferences.updates.status.isChecking");
						case CHECK_SUCCESSFUL -> resourceBundle.getString("preferences.updates.status.checkSuccessful");
						case CHECK_FAILED -> resourceBundle.getString("preferences.updates.status.checkFailed");
					};
				}, updateCheckStateProperty
				));
	}

	@FXML
	public void checkNow() {
		updateChecker.checkForUpdatesNow();
	}

	@FXML
	public void visitDownloadsPage() {
		application.getHostServices().showDocument(DOWNLOADS_URI);
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

	public BooleanBinding updateAvailableProperty() {
		return updateAvailable;
	}

	public boolean isUpdateAvailable() {
		return updateAvailable.get();
	}
}

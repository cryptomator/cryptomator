package org.cryptomator.ui.preferences;

import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.fxapp.UpdateChecker;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;

@PreferencesScoped
public class UpdatesPreferencesController implements FxController {

	private static final String DOWNLOADS_URI = "https://cryptomator.org/downloads";

	private final Application application;
	private final Settings settings;
	private final UpdateChecker updateChecker;
	private final ObjectBinding<ContentDisplay> checkForUpdatesButtonState;
	private final ReadOnlyStringProperty latestVersion;
	private final String currentVersion;
	private final BooleanBinding updateAvailable;
	private final ReadOnlyBooleanProperty upToDate;
	
	/* FXML */
	public CheckBox checkForUpdatesCheckbox;

	@Inject
	UpdatesPreferencesController(Application application, Settings settings, UpdateChecker updateChecker) {
		this.application = application;
		this.settings = settings;
		this.updateChecker = updateChecker;
		this.checkForUpdatesButtonState = Bindings.when(updateChecker.checkingForUpdatesProperty()).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
		this.latestVersion = updateChecker.latestVersionProperty();
		this.updateAvailable = latestVersion.isNotNull();
		this.currentVersion = updateChecker.getCurrentVersion();
		this.upToDate = updateChecker.upToDateProperty();
	}

	public void initialize() {
		checkForUpdatesCheckbox.selectedProperty().bindBidirectional(settings.checkForUpdates);
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

	public ReadOnlyBooleanProperty upToDateProperty(){ return upToDate;}

	public boolean getUpToDate(){ return upToDate.get();}
}

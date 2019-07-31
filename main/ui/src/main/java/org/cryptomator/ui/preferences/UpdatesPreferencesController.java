package org.cryptomator.ui.preferences;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javax.inject.Named;

@PreferencesScoped
public class UpdatesPreferencesController implements FxController {

	private final Settings settings;
	private final ObjectBinding<ContentDisplay> checkForUpdatesButtonState;
	public CheckBox checkForUpdatesCheckbox;

	@Inject
	UpdatesPreferencesController(Settings settings, @Named("checkingForUpdates") ReadOnlyBooleanProperty checkingForUpdates) {
		this.settings = settings;
		this.checkForUpdatesButtonState = Bindings.when(checkingForUpdates).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
	}

	public void initialize() {
		checkForUpdatesCheckbox.selectedProperty().bindBidirectional(settings.checkForUpdates());
	}

	@FXML
	public void checkNow() {
	}

	/* Getter/Setter */

	public ObjectBinding<ContentDisplay> checkForUpdatesButtonStateProperty() {
		return checkForUpdatesButtonState;
	}

	public ContentDisplay getCheckForUpdatesButtonState() {
		return checkForUpdatesButtonState.get();
	}
}

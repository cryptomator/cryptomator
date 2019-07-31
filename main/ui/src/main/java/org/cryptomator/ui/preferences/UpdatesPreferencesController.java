package org.cryptomator.ui.preferences;

import javafx.scene.control.CheckBox;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;

@PreferencesScoped
public class UpdatesPreferencesController implements FxController {

	private final Settings settings;
	public CheckBox checkForUpdatesCheckbox;

	@Inject
	UpdatesPreferencesController(Settings settings) {
		this.settings = settings;
	}

	public void initialize() {
		checkForUpdatesCheckbox.selectedProperty().bindBidirectional(settings.checkForUpdates());
	}

}

package org.cryptomator.ui.preferences;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.ChoiceBox;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.VolumeImpl;
import org.cryptomator.ui.FxController;
import org.cryptomator.ui.model.Volume;

import javax.inject.Inject;

@PreferencesWindow
public class PreferencesController implements FxController {
	
	private final Settings settings;
	private final BooleanBinding showWebDavSettings;
	public ChoiceBox<VolumeImpl> volumeTypeChoicBox;

	@Inject
	PreferencesController(Settings settings) {
		this.settings = settings;
		this.showWebDavSettings = Bindings.equal(settings.preferredVolumeImpl(), VolumeImpl.WEBDAV);
	}

	public void initialize() {
		volumeTypeChoicBox.getItems().addAll(Volume.getCurrentSupportedAdapters());
		volumeTypeChoicBox.valueProperty().bindBidirectional(settings.preferredVolumeImpl());
	}

	public BooleanBinding showWebDavSettingsProperty() {
		return showWebDavSettings;
	}

	public Boolean getShowWebDavSettings() {
		return showWebDavSettings.get();
	}
}

package org.cryptomator.ui.preferences;

import dagger.Lazy;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.mount.MountCapability;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ChoiceBox;
import javafx.util.StringConverter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@PreferencesScoped
public class VolumePreferencesController implements FxController {

	private static final String DOCS_MOUNTING_URL = "https://docs.cryptomator.org/en/1.7/desktop/volume-type/";

	private final Settings settings;
	private final ObservableValue<MountService> selectedMountService;
	private final ResourceBundle resourceBundle;
	private final ObservableValue<Boolean> mountToDirSupported;
	private final ObservableValue<Boolean> mountToDriveLetterSupported;
	private final ObservableValue<Boolean> mountFlagsSupported;
	private final ObservableValue<Boolean> readonlySupported;
	private final Lazy<Application> application;
	private final List<MountService> mountProviders;
	public ChoiceBox<MountService> volumeTypeChoiceBox;

	@Inject
	VolumePreferencesController(Settings settings,
								Lazy<Application> application,
								List<MountService> mountProviders,
								ResourceBundle resourceBundle) {
		this.settings = settings;
		this.application = application;
		this.mountProviders = mountProviders;
		this.resourceBundle = resourceBundle;

		var fallbackProvider = mountProviders.stream().findFirst().orElse(null);
		this.selectedMountService = ObservableUtil.mapWithDefault(settings.mountService, serviceName -> mountProviders.stream().filter(s -> s.getClass().getName().equals(serviceName)).findFirst().orElse(fallbackProvider), fallbackProvider);
		this.mountToDirSupported = selectedMountService.map(s -> s.hasCapability(MountCapability.MOUNT_WITHIN_EXISTING_PARENT) || s.hasCapability(MountCapability.MOUNT_TO_EXISTING_DIR));
		this.mountToDriveLetterSupported = selectedMountService.map(s -> s.hasCapability(MountCapability.MOUNT_AS_DRIVE_LETTER));
		this.mountFlagsSupported = selectedMountService.map(s -> s.hasCapability(MountCapability.MOUNT_FLAGS));
		this.readonlySupported = selectedMountService.map(s -> s.hasCapability(MountCapability.READ_ONLY));
	}

	public void initialize() {
		volumeTypeChoiceBox.getItems().add(null);
		volumeTypeChoiceBox.getItems().addAll(mountProviders);
		volumeTypeChoiceBox.setConverter(new MountServiceConverter());
		boolean autoSelected = settings.mountService.get() == null;
		volumeTypeChoiceBox.getSelectionModel().select(autoSelected ? null : selectedMountService.getValue());
		volumeTypeChoiceBox.valueProperty().addListener((observableValue, oldProvider, newProvider) -> {
			var toSet = Optional.ofNullable(newProvider).map(nP -> nP.getClass().getName()).orElse(null);
			settings.mountService.set(toSet);
		});
	}

	/* Property Getters */
	public ObservableValue<Boolean> readonlySupportedProperty() {
		return readonlySupported;
	}

	public boolean isReadonlySupported() {
		return readonlySupported.getValue();
	}

	public ObservableValue<Boolean> mountToDirSupportedProperty() {
		return mountToDirSupported;
	}

	public boolean isMountToDirSupported() {
		return mountToDirSupported.getValue();
	}

	public ObservableValue<Boolean> mountToDriveLetterSupportedProperty() {
		return mountToDriveLetterSupported;
	}

	public boolean isMountToDriveLetterSupported() {
		return mountToDriveLetterSupported.getValue();
	}

	public ObservableValue<Boolean> mountFlagsSupportedProperty() {
		return mountFlagsSupported;
	}

	public boolean isMountFlagsSupported() {
		return mountFlagsSupported.getValue();
	}

	/* Helpers */

	private class MountServiceConverter extends StringConverter<MountService> {

		@Override
		public String toString(MountService provider) {
			if (provider == null) {
				return resourceBundle.getString("preferences.volume.type.automatic");
			} else {
				return provider.displayName();
			}
		}

		@Override
		public MountService fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}

	public void openDocs() {
		application.get().getHostServices().showDocument(DOCS_MOUNTING_URL);
	}
}

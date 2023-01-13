package org.cryptomator.ui.preferences;

import org.cryptomator.common.mount.ActualMountService;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.integrations.mount.MountCapability;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import java.util.List;

/**
 * TODO: if WebDAV is selected under Windows, show warning that specific mount options (like selecting a directory as mount point) are _not_ supported
 */
@PreferencesScoped
public class VolumePreferencesController implements FxController {

	private final Settings settings;
	private final ObservableValue<ActualMountService> selectedMountService;
	private final BooleanExpression loopbackPortSupported;
	private final ObservableValue<Boolean> mountToDirSupported;
	private final ObservableValue<Boolean> mountToDriveLetterSupported;
	private final ObservableValue<Boolean> mountFlagsSupported;
	private final ObservableValue<Boolean> readonlySupported;
	private final List<MountService> mountProviders;
	public ChoiceBox<MountService> volumeTypeChoiceBox;
	public TextField loopbackPortField;
	public Button loopbackPortApplyButton;

	@Inject
	VolumePreferencesController(Settings settings, List<MountService> mountProviders, ObservableValue<ActualMountService> actualMountService) {
		this.settings = settings;
		this.mountProviders = mountProviders;
		this.selectedMountService = actualMountService;
		this.loopbackPortSupported = BooleanExpression.booleanExpression(selectedMountService.map(as -> as.service().hasCapability(MountCapability.LOOPBACK_PORT)));
		this.mountToDirSupported = selectedMountService.map(as -> as.service().hasCapability(MountCapability.MOUNT_WITHIN_EXISTING_PARENT) || as.service().hasCapability(MountCapability.MOUNT_TO_EXISTING_DIR));
		this.mountToDriveLetterSupported = selectedMountService.map(as -> as.service().hasCapability(MountCapability.MOUNT_AS_DRIVE_LETTER));
		this.mountFlagsSupported = selectedMountService.map(as -> as.service().hasCapability(MountCapability.MOUNT_FLAGS));
		this.readonlySupported = selectedMountService.map(as -> as.service().hasCapability(MountCapability.READ_ONLY));
	}

	public void initialize() {
		volumeTypeChoiceBox.getItems().addAll(mountProviders);
		volumeTypeChoiceBox.setConverter(new MountServiceConverter());
		volumeTypeChoiceBox.getSelectionModel().select(selectedMountService.getValue().service());
		volumeTypeChoiceBox.valueProperty().addListener((observableValue, oldProvide, newProvider) -> settings.mountService().set(newProvider.getClass().getName()));

		loopbackPortField.setText(String.valueOf(settings.port().get()));
		loopbackPortApplyButton.visibleProperty().bind(settings.port().asString().isNotEqualTo(loopbackPortField.textProperty()));
		loopbackPortApplyButton.disableProperty().bind(Bindings.createBooleanBinding(this::validateLoopbackPort, loopbackPortField.textProperty()).not());
	}

	private boolean validateLoopbackPort() {
		try {
			int port = Integer.parseInt(loopbackPortField.getText());
			return port == 0 // choose port automatically
					|| port >= Settings.MIN_PORT && port <= Settings.MAX_PORT; // port within range
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public void doChangeLoopbackPort() {
		if (validateLoopbackPort()) {
			settings.port().set(Integer.parseInt(loopbackPortField.getText()));
		}
	}

	/* Property Getters */

	public BooleanExpression loopbackPortSupportedProperty() {
		return loopbackPortSupported;
	}

	public boolean isLoopbackPortSupported() {
		return loopbackPortSupported.get();
	}

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

	private static class MountServiceConverter extends StringConverter<MountService> {

		@Override
		public String toString(MountService provider) {
			return provider.displayName();
		}

		@Override
		public MountService fromString(String string) {
			throw new UnsupportedOperationException();
		}
	}
}

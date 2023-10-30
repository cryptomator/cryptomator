package org.cryptomator.ui.preferences;

import dagger.Lazy;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.VaultModule;
import org.cryptomator.integrations.mount.MountCapability;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.ui.common.FxController;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

@PreferencesScoped
public class VolumePreferencesController implements FxController {

	private static final String DOCS_MOUNTING_URL = "https://docs.cryptomator.org/en/1.7/desktop/volume-type/";
	private static final int MIN_PORT = 1024;
	private static final int MAX_PORT = 65535;

	private final Settings settings;
	private final ObservableValue<MountService> selectedMountService;
	private final ResourceBundle resourceBundle;
	private final BooleanExpression loopbackPortSupported;
	private final ObservableValue<Boolean> mountToDirSupported;
	private final ObservableValue<Boolean> mountToDriveLetterSupported;
	private final ObservableValue<Boolean> mountFlagsSupported;
	private final ObservableValue<Boolean> readonlySupported;
	private final ObservableValue<Boolean> fuseRestartRequired;
	private final Lazy<Application> application;
	private final List<MountService> mountProviders;
	public ChoiceBox<MountService> volumeTypeChoiceBox;
	public TextField loopbackPortField;
	public Button loopbackPortApplyButton;

	@Inject
	VolumePreferencesController(Settings settings, Lazy<Application> application, List<MountService> mountProviders, @Named("FUPFMS") AtomicReference<MountService> firstUsedProblematicFuseMountService, ResourceBundle resourceBundle) {
		this.settings = settings;
		this.application = application;
		this.mountProviders = mountProviders;
		this.resourceBundle = resourceBundle;

		var fallbackProvider = mountProviders.stream().findFirst().orElse(null);
		this.selectedMountService = ObservableUtil.mapWithDefault(settings.mountService, serviceName -> mountProviders.stream().filter(s -> s.getClass().getName().equals(serviceName)).findFirst().orElse(fallbackProvider), fallbackProvider);
		this.loopbackPortSupported = BooleanExpression.booleanExpression(selectedMountService.map(s -> s.hasCapability(MountCapability.LOOPBACK_PORT)));
		this.mountToDirSupported = selectedMountService.map(s -> s.hasCapability(MountCapability.MOUNT_WITHIN_EXISTING_PARENT) || s.hasCapability(MountCapability.MOUNT_TO_EXISTING_DIR));
		this.mountToDriveLetterSupported = selectedMountService.map(s -> s.hasCapability(MountCapability.MOUNT_AS_DRIVE_LETTER));
		this.mountFlagsSupported = selectedMountService.map(s -> s.hasCapability(MountCapability.MOUNT_FLAGS));
		this.readonlySupported = selectedMountService.map(s -> s.hasCapability(MountCapability.READ_ONLY));
		this.fuseRestartRequired = selectedMountService.map(s -> //
				firstUsedProblematicFuseMountService.get() != null //
					&& VaultModule.isProblematicFuseService(s) //
					&& !firstUsedProblematicFuseMountService.get().equals(s)
		);
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

		loopbackPortField.setText(String.valueOf(settings.port.get()));
		loopbackPortApplyButton.visibleProperty().bind(settings.port.asString().isNotEqualTo(loopbackPortField.textProperty()));
		loopbackPortApplyButton.disableProperty().bind(Bindings.createBooleanBinding(this::validateLoopbackPort, loopbackPortField.textProperty()).not());
	}

	private boolean validateLoopbackPort() {
		try {
			int port = Integer.parseInt(loopbackPortField.getText());
			return port == 0 // choose port automatically
					|| port >= MIN_PORT && port <= MAX_PORT; // port within range
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public void doChangeLoopbackPort() {
		if (validateLoopbackPort()) {
			settings.port.set(Integer.parseInt(loopbackPortField.getText()));
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

	public ObservableValue<Boolean> fuseRestartRequiredProperty() {
		return fuseRestartRequired;
	}

	public boolean getFuseRestartRequired() {
		return fuseRestartRequired.getValue();
	}

	/* Helpers */

	public class MountServiceConverter extends StringConverter<MountService> {

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

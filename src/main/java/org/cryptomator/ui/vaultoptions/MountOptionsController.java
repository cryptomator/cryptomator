package org.cryptomator.ui.vaultoptions;

import com.google.common.base.Strings;
import dagger.Lazy;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.mount.MountServiceConverter;
import org.cryptomator.common.mount.WindowsDriveLetters;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
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
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@VaultOptionsScoped
public class MountOptionsController implements FxController {

	private static final String DOCS_MOUNTING_URL = "https://docs.cryptomator.org/en/1.7/desktop/volume-type/";
	private static final int MIN_PORT = 1024;
	private static final int MAX_PORT = 65535;

	private final Stage window;
	private final VaultSettings vaultSettings;
	private final WindowsDriveLetters windowsDriveLetters;
	private final ResourceBundle resourceBundle;
	private final Lazy<Application> application;

	private final ObservableValue<String> defaultMountFlags;
	private final ObservableValue<Boolean> mountpointDirSupported;
	private final ObservableValue<Boolean> mountpointDriveLetterSupported;
	private final ObservableValue<Boolean> readOnlySupported;
	private final ObservableValue<Boolean> mountFlagsSupported;
	private final ObservableValue<String> directoryPath;
	private final List<MountService> mountProviders;
	private final ObservableValue<MountService> selectedMountService;
	private final ObservableValue<Boolean> fuseRestartRequired;
	private final BooleanExpression loopbackPortSupported;


	//-- FXML objects --
	public CheckBox readOnlyCheckbox;
	public CheckBox customMountFlagsCheckbox;
	public TextField mountFlagsField;
	public ToggleGroup mountPointToggleGroup;
	public RadioButton mountPointAutoBtn;
	public RadioButton mountPointDriveLetterBtn;
	public RadioButton mountPointDirBtn;
	public TextField directoryPathField;
	public ChoiceBox<Path> driveLetterSelection;
	public ChoiceBox<MountService> vaultVolumeTypeChoiceBox;
	public TextField vaultLoopbackPortField;
	public Button vaultLoopbackPortApplyButton;


	@Inject
	MountOptionsController(@VaultOptionsWindow Stage window, //
						   @VaultOptionsWindow Vault vault, //
						   WindowsDriveLetters windowsDriveLetters, //
						   ResourceBundle resourceBundle, //
						   Lazy<Application> application, List<MountService> mountProviders, //
						   @Named("FUPFMS") AtomicReference<MountService> firstUsedProblematicFuseMountService) {
		this.window = window;
		this.vaultSettings = vault.getVaultSettings();
		this.windowsDriveLetters = windowsDriveLetters;
		this.resourceBundle = resourceBundle;
		this.directoryPath = vault.getVaultSettings().mountPoint.map(p -> isDriveLetter(p) ? null : p.toString());
		this.application = application;
		this.mountProviders = mountProviders;
		var fallbackProvider = mountProviders.stream().findFirst().orElse(null);
		this.selectedMountService = ObservableUtil.mapWithDefault(vaultSettings.mountService, serviceName -> mountProviders.stream().filter(s -> s.getClass().getName().equals(serviceName)).findFirst().orElse(fallbackProvider), fallbackProvider);
		this.fuseRestartRequired = selectedMountService.map(s -> firstUsedProblematicFuseMountService.get() != null && VaultModule.isProblematicFuseService(s) && !firstUsedProblematicFuseMountService.get().equals(s));
		this.loopbackPortSupported = BooleanExpression.booleanExpression(selectedMountService.map(s -> s.hasCapability(MountCapability.LOOPBACK_PORT)));

		this.defaultMountFlags = selectedMountService.map(s -> {
			if (s.hasCapability(MountCapability.MOUNT_FLAGS)) {
				return s.getDefaultMountFlags();
			} else {
				return "";
			}
		});
		this.mountFlagsSupported = selectedMountService.map(s -> s.hasCapability(MountCapability.MOUNT_FLAGS));
		this.readOnlySupported = selectedMountService.map(s -> s.hasCapability(MountCapability.READ_ONLY));
		this.mountpointDirSupported = selectedMountService.map(s -> s.hasCapability(MountCapability.MOUNT_TO_EXISTING_DIR) || s.hasCapability(MountCapability.MOUNT_WITHIN_EXISTING_PARENT));
		this.mountpointDriveLetterSupported = selectedMountService.map(s -> s.hasCapability(MountCapability.MOUNT_AS_DRIVE_LETTER));
	}

	@FXML
	public void initialize() {
		// readonly:
		readOnlyCheckbox.selectedProperty().bindBidirectional(vaultSettings.usesReadOnlyMode);

		// custom mount flags:
		mountFlagsField.disableProperty().bind(customMountFlagsCheckbox.selectedProperty().not());
		customMountFlagsCheckbox.setSelected(!Strings.isNullOrEmpty(vaultSettings.mountFlags.getValue()));
		toggleUseCustomMountFlags();

		//driveLetter choice box
		driveLetterSelection.getItems().addAll(windowsDriveLetters.getAll());
		driveLetterSelection.setConverter(new WinDriveLetterLabelConverter(windowsDriveLetters, resourceBundle));

		//mountPoint toggle group
		var mountPoint = vaultSettings.mountPoint.get();
		if (mountPoint == null) {
			//prepare and select auto
			mountPointToggleGroup.selectToggle(mountPointAutoBtn);
		} else if (mountPoint.getParent() == null && isDriveLetter(mountPoint)) {
			//prepare and select drive letter
			mountPointToggleGroup.selectToggle(mountPointDriveLetterBtn);
			driveLetterSelection.valueProperty().bindBidirectional(vaultSettings.mountPoint);
		} else {
			//prepare and select dir
			mountPointToggleGroup.selectToggle(mountPointDirBtn);
		}
		mountPointToggleGroup.selectedToggleProperty().addListener(this::selectedToggleChanged);

		vaultVolumeTypeChoiceBox.getItems().add(null);
		vaultVolumeTypeChoiceBox.getItems().addAll(mountProviders);
		vaultVolumeTypeChoiceBox.setConverter(new MountServiceConverter(resourceBundle));
		boolean autoSelected = vaultSettings.mountService.get() == null;
		vaultVolumeTypeChoiceBox.getSelectionModel().select(autoSelected ? null : selectedMountService.getValue());
		vaultVolumeTypeChoiceBox.valueProperty().addListener((observableValue, oldProvider, newProvider) -> {
			var toSet = Optional.ofNullable(newProvider).map(nP -> nP.getClass().getName()).orElse(null);
			vaultSettings.mountService.set(toSet);
		});

		vaultLoopbackPortField.setText(String.valueOf(vaultSettings.port.get()));
		vaultLoopbackPortApplyButton.visibleProperty().bind(vaultSettings.port.asString().isNotEqualTo(vaultLoopbackPortField.textProperty()));
		vaultLoopbackPortApplyButton.disableProperty().bind(Bindings.createBooleanBinding(this::validateLoopbackPort, vaultLoopbackPortField.textProperty()).not());

	}

	@FXML
	public void toggleUseCustomMountFlags() {
		if (customMountFlagsCheckbox.isSelected()) {
			readOnlyCheckbox.setSelected(false); // to prevent invalid states
			mountFlagsField.textProperty().unbind();
			var mountFlags = vaultSettings.mountFlags.get();
			if (mountFlags == null || mountFlags.isBlank()) {
				vaultSettings.mountFlags.set(defaultMountFlags.getValue());
			}
			mountFlagsField.textProperty().bindBidirectional(vaultSettings.mountFlags);
		} else {
			mountFlagsField.textProperty().unbindBidirectional(vaultSettings.mountFlags);
			vaultSettings.mountFlags.set(null);
			mountFlagsField.textProperty().bind(defaultMountFlags);
		}
	}

	@FXML
	public void chooseCustomMountPoint() {
		try {
			Path chosenPath = chooseCustomMountPointInternal();
			vaultSettings.mountPoint.set(chosenPath);
		} catch (NoDirSelectedException e) {
			//no-op
		}
	}

	/**
	 * Prepares and opens a directory chooser dialog.
	 * This method blocks until the dialog is closed.
	 *
	 * @return the absolute path to the chosen directory
	 * @throws NoDirSelectedException if dialog is closed without choosing a directory
	 */
	private Path chooseCustomMountPointInternal() throws NoDirSelectedException {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resourceBundle.getString("vaultOptions.mount.mountPoint.directoryPickerTitle"));
		try {
			var mp = vaultSettings.mountPoint.get();
			var initialDir = mp != null && !isDriveLetter(mp) ? mp : Path.of(System.getProperty("user.home"));

			if (Files.isDirectory(initialDir)) {
				directoryChooser.setInitialDirectory(initialDir.toFile());
			}
		} catch (InvalidPathException e) {
			// no-op
		}
		File file = directoryChooser.showDialog(window);
		if (file != null) {
			return file.toPath();
		} else {
			throw new NoDirSelectedException();
		}
	}

	private void selectedToggleChanged(ObservableValue<? extends Toggle> observable, Toggle oldToggle, Toggle newToggle) {
		//Remark: the mountpoint corresponding to the newToggle must be null, otherwise it would not be new!
		driveLetterSelection.valueProperty().unbindBidirectional(vaultSettings.mountPoint);
		if (mountPointDriveLetterBtn.equals(newToggle)) {
			vaultSettings.mountPoint.set(windowsDriveLetters.getFirstDesiredAvailable().orElse(windowsDriveLetters.getAll().stream().findAny().get()));
			driveLetterSelection.valueProperty().bindBidirectional(vaultSettings.mountPoint);
		} else if (mountPointDirBtn.equals(newToggle)) {
			try {
				vaultSettings.mountPoint.set(chooseCustomMountPointInternal());
			} catch (NoDirSelectedException e) {
				if (oldToggle != null && !mountPointDirBtn.equals(oldToggle)) {
					mountPointToggleGroup.selectToggle(oldToggle);
				} else {
					mountPointToggleGroup.selectToggle(mountPointAutoBtn);
				}
			}
		} else {
			vaultSettings.mountPoint.set(null);
		}
	}

	private boolean isDriveLetter(Path mountPoint) {
		if (mountPoint != null) {
			var s = mountPoint.toString();
			return s.length() == 3 && s.endsWith(":\\");
		}
		return false;
	}

	private static class WinDriveLetterLabelConverter extends StringConverter<Path> {

		private final Set<Path> occupiedDriveLetters;
		private final ResourceBundle resourceBundle;

		WinDriveLetterLabelConverter(WindowsDriveLetters windowsDriveLetters, ResourceBundle resourceBundle) {
			this.occupiedDriveLetters = windowsDriveLetters.getOccupied();
			this.resourceBundle = resourceBundle;
		}

		@Override
		public String toString(Path driveLetter) {
			if (driveLetter == null) {
				return "";
			} else if (occupiedDriveLetters.contains(driveLetter)) {
				return driveLetter.toString().substring(0, 2) + " (" + resourceBundle.getString("vaultOptions.mount.winDriveLetterOccupied") + ")";
			} else {
				return driveLetter.toString().substring(0, 2);
			}
		}

		@Override
		public Path fromString(String string) {
			if (string.isEmpty()) {
				return null;
			} else {
				return Path.of(string + "\\");
			}
		}

	}

	public void openDocs() {
		application.get().getHostServices().showDocument(DOCS_MOUNTING_URL);
	}

	private boolean validateLoopbackPort() {
		try {
			int port = Integer.parseInt(vaultLoopbackPortField.getText());
			return port == 0 // choose port automatically
					|| port >= MIN_PORT && port <= MAX_PORT; // port within range
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public void doChangeLoopbackPort() {
		if (validateLoopbackPort()) {
			vaultSettings.port.set(Integer.parseInt(vaultLoopbackPortField.getText()));
		}
	}

	//@formatter:off
	private static class NoDirSelectedException extends Exception {}
	//@formatter:on

	// Getter & Setter

	public ObservableValue<Boolean> mountFlagsSupportedProperty() {
		return mountFlagsSupported;
	}

	public boolean isMountFlagsSupported() {
		return mountFlagsSupported.getValue();
	}

	public ObservableValue<Boolean> mountpointDirSupportedProperty() {
		return mountpointDirSupported;
	}

	public boolean isMountpointDirSupported() {
		return mountpointDirSupported.getValue();
	}

	public ObservableValue<Boolean> mountpointDriveLetterSupportedProperty() {
		return mountpointDriveLetterSupported;
	}

	public boolean isMountpointDriveLetterSupported() {
		return mountpointDriveLetterSupported.getValue();
	}

	public ObservableValue<Boolean> readOnlySupportedProperty() {
		return readOnlySupported;
	}

	public boolean isReadOnlySupported() {
		return readOnlySupported.getValue();
	}

	public ObservableValue<String> directoryPathProperty() {
		return directoryPath;
	}

	public String getDirectoryPath() {
		return directoryPath.getValue();
	}

	public ObservableValue<Boolean> fuseRestartRequiredProperty() {
		return fuseRestartRequired;
	}

	public boolean getFuseRestartRequired() {
		return fuseRestartRequired.getValue();
	}

	public BooleanExpression loopbackPortSupportedProperty() {
		return loopbackPortSupported;
	}

	public boolean isLoopbackPortSupported() {
		return loopbackPortSupported.get();
	}

}

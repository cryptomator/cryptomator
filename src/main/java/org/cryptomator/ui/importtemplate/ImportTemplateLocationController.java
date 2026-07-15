package org.cryptomator.ui.importtemplate;

import dagger.Lazy;
import org.cryptomator.common.ObservableUtil;
import org.cryptomator.common.locationpresets.LocationPreset;
import org.cryptomator.common.locationpresets.LocationPresetsProvider;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.launcher.VaultTemplateExtractor;
import org.cryptomator.launcher.VaultTemplateExtractor.MalformedTemplateException;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.Tasks;
import org.cryptomator.ui.controls.FontAwesome5IconView;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

@ImportTemplateScoped
public class ImportTemplateLocationController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ImportTemplateLocationController.class);
	private static final Path DEFAULT_CUSTOM_VAULT_PATH = Path.of(System.getProperty("user.home"));
	private static final String TEMP_FILE_PREFIX = ".locationTest.cryptomator";

	private final Stage window;
	private final String vaultName;
	private final byte[] template;
	private final ObjectProperty<Path> vaultPath;
	private final ObjectProperty<Vault> vault;
	private final Lazy<Scene> successScene;
	private final FxApplicationWindows appWindows;
	private final VaultListManager vaultListManager;
	private final ExecutorService executor;
	private final Settings settings;
	private final ResourceBundle resourceBundle;
	private final Dialogs dialogs;
	private final ObservableValue<VaultPathStatus> vaultPathStatus;
	private final ObservableValue<Boolean> validVaultPath;
	private final BooleanProperty usePresetPath;
	private final BooleanProperty loadingPresetLocations = new SimpleBooleanProperty(false);
	private final BooleanProperty processing = new SimpleBooleanProperty(false);
	private final ObservableValue<Boolean> readyToImport;
	private final ObjectBinding<ContentDisplay> createButtonState;
	private final ObservableList<Node> radioButtons;
	private final ObservableList<Node> sortedRadioButtons;

	private Path customVaultPath = DEFAULT_CUSTOM_VAULT_PATH;

	//FXML
	public ToggleGroup locationPresetsToggler;
	public VBox radioButtonVBox;
	public HBox customLocationRadioBtn;
	public RadioButton customRadioButton;
	public Label locationStatusLabel;
	public FontAwesome5IconView goodLocation;
	public FontAwesome5IconView badLocation;

	@Inject
	ImportTemplateLocationController(@ImportTemplateWindow Stage window, //
									 @Named("vaultName") String vaultName, //
									 @Named("vaultTemplate") byte[] template, //
									 ObjectProperty<Path> vaultPath, //
									 @ImportTemplateWindow ObjectProperty<Vault> vault, //
									 @FxmlScene(FxmlFile.IMPORT_TEMPLATE_SUCCESS) Lazy<Scene> successScene, //
									 FxApplicationWindows appWindows, //
									 VaultListManager vaultListManager, //
									 ExecutorService executor, //
									 Settings settings, //
									 ResourceBundle resourceBundle, //
									 Dialogs dialogs) {
		this.window = window;
		this.vaultName = vaultName;
		this.template = template;
		this.vaultPath = vaultPath;
		this.vault = vault;
		this.successScene = successScene;
		this.appWindows = appWindows;
		this.vaultListManager = vaultListManager;
		this.executor = executor;
		this.settings = settings;
		this.resourceBundle = resourceBundle;
		this.dialogs = dialogs;
		this.vaultPathStatus = ObservableUtil.mapWithDefault(vaultPath, this::validatePath, new VaultPathStatus(false, "error.message"));
		this.validVaultPath = ObservableUtil.mapWithDefault(vaultPathStatus, VaultPathStatus::valid, false);
		this.vaultPathStatus.addListener(this::updateStatusLabel);
		this.usePresetPath = new SimpleBooleanProperty();
		this.readyToImport = Bindings.createBooleanBinding(() -> validVaultPath.getValue() && !processing.get(), validVaultPath, processing);
		this.createButtonState = Bindings.when(processing).then(ContentDisplay.LEFT).otherwise(ContentDisplay.TEXT_ONLY);
		this.radioButtons = FXCollections.observableArrayList();
		this.sortedRadioButtons = radioButtons.sorted(this::compareLocationPresets);

		Path previouslyUsedDirectory = settings.previouslyUsedVaultDirectory.get();
		if (previouslyUsedDirectory != null) {
			try {
				if (Files.exists(previouslyUsedDirectory) && Files.isDirectory(previouslyUsedDirectory) && isActuallyWritable(previouslyUsedDirectory)) {
					this.customVaultPath = previouslyUsedDirectory;
				}
			} catch (InvalidPathException | NullPointerException e) {
				LOG.warn("Invalid previously used vault directory path: {}", previouslyUsedDirectory, e);
			}
		}
	}

	private VaultPathStatus validatePath(Path p) throws NullPointerException {
		if (!Files.exists(p.getParent())) {
			return new VaultPathStatus(false, "addvaultwizard.new.locationDoesNotExist");
		} else if (!isActuallyWritable(p.getParent())) {
			return new VaultPathStatus(false, "addvaultwizard.new.locationIsNotWritable");
		} else if (!Files.notExists(p)) {
			return new VaultPathStatus(false, "addvaultwizard.new.fileAlreadyExists");
		} else {
			return new VaultPathStatus(true, "addvaultwizard.new.locationIsOk");
		}
	}

	private void updateStatusLabel(ObservableValue<? extends VaultPathStatus> observable, VaultPathStatus oldValue, VaultPathStatus newValue) {
		if (newValue.valid()) {
			locationStatusLabel.setGraphic(goodLocation);
			locationStatusLabel.getStyleClass().remove("label-red");
			locationStatusLabel.getStyleClass().add("label-muted");
		} else {
			locationStatusLabel.setGraphic(badLocation);
			locationStatusLabel.getStyleClass().remove("label-muted");
			locationStatusLabel.getStyleClass().add("label-red");
		}
		this.locationStatusLabel.setText(resourceBundle.getString(newValue.localizationKey()));
	}

	private boolean isActuallyWritable(Path p) {
		Path tmpDir = null;
		try {
			tmpDir = Files.createTempDirectory(p, TEMP_FILE_PREFIX);
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			if (tmpDir != null) {
				try {
					Files.deleteIfExists(tmpDir);
				} catch (IOException e) {
					LOG.warn("Unable to delete temporary directory {}. Needs to be deleted manually.", tmpDir);
				}
			}
		}
	}

	@FXML
	public void initialize() {
		var task = executor.submit(this::loadLocationPresets);
		window.addEventHandler(WindowEvent.WINDOW_HIDING, _ -> task.cancel(true));
		locationPresetsToggler.selectedToggleProperty().addListener(this::togglePredefinedLocation);
		usePresetPath.bind(locationPresetsToggler.selectedToggleProperty().isNotEqualTo(customRadioButton));
		radioButtons.add(customLocationRadioBtn);
		Bindings.bindContent(radioButtonVBox.getChildren(), sortedRadioButtons); //to prevent garbage collection of the binding, we bind explicitly to the sorted list
	}

	private void loadLocationPresets() {
		Platform.runLater(() -> loadingPresetLocations.set(true));
		try {
			LocationPresetsProvider.loadAll(LocationPresetsProvider.class) //
					.flatMap(LocationPresetsProvider::getLocations) //we do not use sorted(), because it evaluates the stream elements, blocking until all elements are gathered
					.forEach(this::createRadioButtonFor);
		} finally {
			Platform.runLater(() -> loadingPresetLocations.set(false));
		}
	}

	private void createRadioButtonFor(LocationPreset preset) {
		Platform.runLater(() -> {
			var btn = new RadioButton(preset.name());
			btn.setUserData(preset.path());
			radioButtons.add(btn);
			locationPresetsToggler.getToggles().add(btn);
		});
	}

	private int compareLocationPresets(Node left, Node right) {
		if (customLocationRadioBtn.getId().equals(left.getId())) {
			return 1;
		} else if (customLocationRadioBtn.getId().equals(right.getId())) {
			return -1;
		} else {
			return ((RadioButton) left).getText().compareToIgnoreCase(((RadioButton) right).getText());
		}
	}

	private void togglePredefinedLocation(@SuppressWarnings("unused") ObservableValue<? extends Toggle> observable, @SuppressWarnings("unused") Toggle oldValue, Toggle newValue) {
		var storagePath = Optional.ofNullable((Path) newValue.getUserData()).orElse(customVaultPath);
		vaultPath.set(storagePath.resolve(vaultName));
	}

	@FXML
	public void chooseCustomVaultPath() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(resourceBundle.getString("addvaultwizard.new.directoryPickerTitle"));
		if (Files.exists(customVaultPath)) {
			directoryChooser.setInitialDirectory(customVaultPath.toFile());
		} else {
			directoryChooser.setInitialDirectory(DEFAULT_CUSTOM_VAULT_PATH.toFile());
		}
		final File file = directoryChooser.showDialog(window);
		if (file != null) {
			customVaultPath = file.toPath().toAbsolutePath();
			vaultPath.set(customVaultPath.resolve(vaultName));
		}
	}

	@FXML
	public void finish() {
		if (!isReadyToImport()) {
			return;
		}
		Path destination = vaultPath.get();
		processing.set(true);
		Tasks.create(() -> {
			VaultTemplateExtractor.extractAndMove(template, destination);
			return vaultListManager.add(destination);
		}).onSuccess(newVault -> {
			vault.set(newVault);
			rememberParentDirectory(destination);
			window.setScene(successScene.get());
		}).onError(MalformedTemplateException.class, e -> { // must precede the IOException handler: Tasks picks the first matching one
			LOG.error("Vault template is malformed.", e);
			dialogs.prepareMalformedTemplateDialog(window).setOkAction(stage -> {
				stage.close();
				window.close(); //TODO: before showing this dialog, close the window and use the mainWindow as owner
			}).build().showAndWait();
		}).onError(IOException.class, e -> {
			LOG.error("Failed to import vault template.", e);
			appWindows.showErrorWindow(e, window, window.getScene());
		}).andFinally(() -> processing.set(false)).runOnce(executor);
	}

	private void rememberParentDirectory(Path destination) {
		Path parentPath = destination.getParent();
		if (parentPath != null) {
			settings.previouslyUsedVaultDirectory.setValue(parentPath);
		}
	}

	/* Internal classes */

	private record VaultPathStatus(boolean valid, String localizationKey) {

	}

	/* Getter/Setter */

	public String getVaultName() {
		return vaultName;
	}

	public Path getVaultPath() {
		return vaultPath.get();
	}

	public ObjectProperty<Path> vaultPathProperty() {
		return vaultPath;
	}

	public boolean isLoadingPresetLocations() {
		return loadingPresetLocations.getValue();
	}

	public BooleanProperty loadingPresetLocationsProperty() {
		return loadingPresetLocations;
	}

	public BooleanProperty usePresetPathProperty() {
		return usePresetPath;
	}

	public boolean isUsePresetPath() {
		return usePresetPath.get();
	}

	public ObservableValue<Boolean> readyToImportProperty() {
		return readyToImport;
	}

	public boolean isReadyToImport() {
		return readyToImport.getValue();
	}

	public ObjectBinding<ContentDisplay> createButtonStateProperty() {
		return createButtonState;
	}

	public ContentDisplay getCreateButtonState() {
		return createButtonState.get();
	}

	public BooleanBinding anyRadioButtonSelectedProperty() {
		return locationPresetsToggler.selectedToggleProperty().isNotNull();
	}

	public boolean isAnyRadioButtonSelected() {
		return anyRadioButtonSelectedProperty().get();
	}

}

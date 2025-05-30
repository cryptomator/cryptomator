package org.cryptomator.ui.addvaultwizard;

import javax.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_GLOB;
import static org.cryptomator.common.vaults.VaultState.Value.VAULT_CONFIG_MISSING;

import dagger.Lazy;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.recovery.RecoveryActionType;
import org.cryptomator.common.settings.VaultSettings;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultComponent;
import org.cryptomator.common.vaults.VaultConfigCache;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.integrations.mount.MountService;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.dialogs.Dialogs;
import org.cryptomator.ui.fxapp.FxApplicationStyle;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.cryptomator.ui.recoverykey.RecoveryKeyComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AddVaultWizardScoped
public class ChooseExistingVaultController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChooseExistingVaultController.class);

	private final Stage window;
	private final Lazy<Scene> successScene;
	private final FxApplicationWindows appWindows;
	private final ObjectProperty<Path> vaultPath;
	private final ObjectProperty<Vault> vault;
	private final VaultListManager vaultListManager;
	private final ResourceBundle resourceBundle;
	private final ObservableValue<Image> screenshot;
	private final Dialogs dialogs;
	private final VaultComponent.Factory vaultComponentFactory;
	private final RecoveryKeyComponent.Factory recoveryKeyWindow;
	private final List<MountService> mountServices;

	@FXML
	private CheckBox restoreCheckBox;

	private final BooleanProperty restoreButtonVisible = new SimpleBooleanProperty(false);


	@Inject
	ChooseExistingVaultController(@AddVaultWizardWindow Stage window, //
								  @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, //
								  FxApplicationWindows appWindows, //
								  ObjectProperty<Path> vaultPath, //
								  @AddVaultWizardWindow ObjectProperty<Vault> vault, //
								  VaultListManager vaultListManager, //
								  ResourceBundle resourceBundle, //
								  FxApplicationStyle applicationStyle, //
								  RecoveryKeyComponent.Factory recoveryKeyWindow, //
								  VaultComponent.Factory vaultComponentFactory, //
								  List<MountService> mountServices, //
								  Dialogs dialogs) {
		this.window = window;
		this.successScene = successScene;
		this.appWindows = appWindows;
		this.vaultPath = vaultPath;
		this.vault = vault;
		this.vaultListManager = vaultListManager;
		this.resourceBundle = resourceBundle;
		this.screenshot = applicationStyle.appliedThemeProperty().map(this::selectScreenshot);
		this.recoveryKeyWindow = recoveryKeyWindow;
		this.vaultComponentFactory = vaultComponentFactory;
		this.mountServices = mountServices;
		this.dialogs = dialogs;
	}

	public void initialize() {
		restoreButtonVisible.bind(restoreCheckBox.selectedProperty());
	}

	private Image selectScreenshot(Theme theme) {
		String imageResourcePath;
		if (SystemUtils.IS_OS_MAC) {
			imageResourcePath = switch (theme) {
				case LIGHT -> "/img/select-masterkey-mac.png";
				case DARK -> "/img/select-masterkey-mac-dark.png";
			};
		} else {
			imageResourcePath = "/img/select-masterkey-win.png";
		}
		return new Image((Objects.requireNonNull(getClass().getResource(imageResourcePath)).toString()));
	}

	@FXML
	public void chooseFileAndNext() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("addvaultwizard.existing.filePickerTitle"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(resourceBundle.getString("addvaultwizard.existing.filePickerMimeDesc"), CRYPTOMATOR_FILENAME_GLOB));
		File masterkeyFile = fileChooser.showOpenDialog(window);
		if (masterkeyFile != null) {
			vaultPath.setValue(masterkeyFile.toPath().toAbsolutePath().getParent());
			try {
				Vault newVault = vaultListManager.add(vaultPath.get());
				vault.set(newVault);
				window.setScene(successScene.get());
			} catch (IOException e) {
				LOG.error("Failed to open existing vault.", e);
				appWindows.showErrorWindow(e, window, window.getScene());
			}
		}
	}

	@FXML
	public void restoreVaultConfigWithRecoveryKey() {
		DirectoryChooser directoryChooser = new DirectoryChooser();

		File selectedDirectory;
		do {
			selectedDirectory = directoryChooser.showDialog(window);
			boolean hasSubfolderD = new File(selectedDirectory, "d").isDirectory();

			if (!hasSubfolderD) {
				dialogs.prepareNoDDirectorySelectedDialog(window).build().showAndWait();
				selectedDirectory = null;
			}
		} while (selectedDirectory == null);

		Optional<Vault> optionalVault = prepareVault(selectedDirectory,vaultComponentFactory,
				mountServices);
		//TODO: optional raus, und mit error dialog arbeiten (UI kram in UI package!) hier nur fehler werfen
		optionalVault.ifPresent(vault -> {
			ObjectProperty<RecoveryActionType> recoverTypeProperty = new SimpleObjectProperty<>(RecoveryActionType.RESTORE_VAULT_CONFIG);
			recoveryKeyWindow.create(vault, window, recoverTypeProperty).showOnboardingDialogWindow();
		});
	}

	public static Optional<Vault> prepareVault(File selectedDirectory, VaultComponent.Factory vaultComponentFactory, List<MountService> mountServices) {

		Path selectedPath = selectedDirectory.toPath();
		VaultSettings vaultSettings = VaultSettings.withRandomId();
		vaultSettings.path.set(selectedPath);
		if (selectedPath.getFileName() != null) {
			vaultSettings.displayName.set(selectedPath.getFileName().toString());
		} else {
			vaultSettings.displayName.set("defaultVaultName");
		}

		var wrapper = new VaultConfigCache(vaultSettings);
		Vault vault = vaultComponentFactory.create(vaultSettings, wrapper, VAULT_CONFIG_MISSING, null).vault(); //TODO: VAULT_CONFIG_MISSING nicht sicher, stand nochmal überprüfen

		//due to https://github.com/cryptomator/cryptomator/issues/2880#issuecomment-1680313498
		var nameOfWinfspLocalMounter = "org.cryptomator.frontend.fuse.mount.WinFspMountProvider";
		if (SystemUtils.IS_OS_WINDOWS && vaultSettings.path.get().toString().contains("Dropbox") && mountServices.stream().anyMatch(s -> s.getClass().getName().equals(nameOfWinfspLocalMounter))) {
			vaultSettings.mountService.setValue(nameOfWinfspLocalMounter);
		}

		return Optional.of(vault);
	}

	/* Getter */

	public ObservableValue<Image> screenshotProperty() {
		return screenshot;
	}

	public Image getScreenshot() {
		return screenshot.getValue();
	}

	public boolean isRestoreButtonVisible() {
		return restoreButtonVisible.get();
	}

	public BooleanProperty restoreButtonVisibleProperty() {
		return restoreButtonVisible;
	}
}

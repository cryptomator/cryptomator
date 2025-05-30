package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.fxapp.FxApplicationStyle;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.ResourceBundle;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_GLOB;

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

	@Inject
	ChooseExistingVaultController(@AddVaultWizardWindow Stage window, //
								  @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, //
								  FxApplicationWindows appWindows, //
								  ObjectProperty<Path> vaultPath, //
								  @AddVaultWizardWindow ObjectProperty<Vault> vault, //
								  VaultListManager vaultListManager, //
								  ResourceBundle resourceBundle, //
								  FxApplicationStyle applicationStyle) {
		this.window = window;
		this.successScene = successScene;
		this.appWindows = appWindows;
		this.vaultPath = vaultPath;
		this.vault = vault;
		this.vaultListManager = vaultListManager;
		this.resourceBundle = resourceBundle;
		this.screenshot = applicationStyle.appliedThemeProperty().map(this::selectScreenshot);
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

	/* Getter */

	public ObservableValue<Image> screenshotProperty() {
		return screenshot;
	}

	public Image getScreenshot() {
		return screenshot.getValue();
	}


}

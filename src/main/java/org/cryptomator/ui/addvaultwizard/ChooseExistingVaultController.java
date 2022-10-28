package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import static org.cryptomator.common.Constants.CRYPTOMATOR_FILENAME_GLOB;

@AddVaultWizardScoped
public class ChooseExistingVaultController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChooseExistingVaultController.class);

	private final Stage window;
	private final Lazy<Scene> welcomeScene;
	private final Lazy<Scene> successScene;
	private final FxApplicationWindows appWindows;
	private final ObjectProperty<Path> vaultPath;
	private final ObjectProperty<Vault> vault;
	private final VaultListManager vaultListManager;
	private final ResourceBundle resourceBundle;
	private final Settings settings;

	private Image screenshot;

	@Inject
	ChooseExistingVaultController(@AddVaultWizardWindow Stage window,
			@FxmlScene(FxmlFile.ADDVAULT_WELCOME) Lazy<Scene> welcomeScene,
			@FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, FxApplicationWindows appWindows,
			ObjectProperty<Path> vaultPath, @AddVaultWizardWindow ObjectProperty<Vault> vault,
			VaultListManager vaultListManager, ResourceBundle resourceBundle, Settings settings) {
		this.window = window;
		this.welcomeScene = welcomeScene;
		this.successScene = successScene;
		this.appWindows = appWindows;
		this.vaultPath = vaultPath;
		this.vault = vault;
		this.vaultListManager = vaultListManager;
		this.resourceBundle = resourceBundle;
		this.settings = settings;
	}

	@FXML
	public void initialize() {
		if (SystemUtils.IS_OS_MAC) {
			this.screenshot = new Image(
					getClass()
							.getResource("/img/select-masterkey-mac"
									+ (UiTheme.LIGHT == settings.theme().get() ? "-light" : "-dark") + ".png")
							.toString());
		} else {
			this.screenshot = new Image(getClass().getResource("/img/select-masterkey-win.png").toString());
		}
	}

	@FXML
	public void back() {
		window.setScene(welcomeScene.get());
	}

	@FXML
	public void chooseFileAndNext() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(resourceBundle.getString("addvaultwizard.existing.filePickerTitle"));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
				resourceBundle.getString("addvaultwizard.existing.filePickerMimeDesc"), CRYPTOMATOR_FILENAME_GLOB));
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

	public Image getScreenshot() {
		return screenshot;
	}

}

package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.ui.common.ErrorComponent;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ResourceBundle;

@AddVaultWizardScoped
public class ChooseExistingVaultController implements FxController {

	private static final Logger LOG = LoggerFactory.getLogger(ChooseExistingVaultController.class);

	private final Stage window;
	private final Lazy<Scene> welcomeScene;
	private final Lazy<Scene> successScene;
	private final ErrorComponent.Builder errorComponent;
	private final ObjectProperty<Path> vaultPath;
	private final ObjectProperty<Vault> vault;
	private final VaultListManager vaultListManager;
	private final ResourceBundle resourceBundle;

	private Image screenshot;

	@Inject
	ChooseExistingVaultController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_WELCOME) Lazy<Scene> welcomeScene, @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, ErrorComponent.Builder errorComponent, ObjectProperty<Path> vaultPath, @AddVaultWizardWindow ObjectProperty<Vault> vault, VaultListManager vaultListManager, ResourceBundle resourceBundle) {
		this.window = window;
		this.welcomeScene = welcomeScene;
		this.successScene = successScene;
		this.errorComponent = errorComponent;
		this.vaultPath = vaultPath;
		this.vault = vault;
		this.vaultListManager = vaultListManager;
		this.resourceBundle = resourceBundle;
	}

	@FXML
	public void initialize() {
		final String resource = SystemUtils.IS_OS_MAC ? "/img/select-masterkey-mac.png" : "/img/select-masterkey-win.png";
		try (InputStream in = getClass().getResourceAsStream(resource)) {
			this.screenshot = new Image(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
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
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Cryptomator Masterkey", "*.cryptomator"));
		File masterkeyFile = fileChooser.showOpenDialog(window);
		if (masterkeyFile != null) {
			vaultPath.setValue(masterkeyFile.toPath().toAbsolutePath().getParent());
			try {
				Vault newVault = vaultListManager.add(vaultPath.get());
				vault.set(newVault);
				window.setScene(successScene.get());
			} catch (NoSuchFileException e) {
				LOG.error("Failed to open existing vault.", e);
				errorComponent.cause(e).window(window).returnToScene(window.getScene()).build().showErrorScene();
			}
		}
	}

	/* Getter */

	public Image getScreenshot() {
		return screenshot;
	}

}

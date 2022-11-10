package org.cryptomator.ui.addvaultwizard;

import dagger.Lazy;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.LicenseHolder;
import org.cryptomator.common.settings.Settings;
import org.cryptomator.common.settings.UiTheme;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.common.vaults.VaultListManager;
import org.cryptomator.integrations.uiappearance.Theme;
import org.cryptomator.integrations.uiappearance.UiAppearanceException;
import org.cryptomator.integrations.uiappearance.UiAppearanceListener;
import org.cryptomator.integrations.uiappearance.UiAppearanceProvider;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.fxapp.FxApplicationWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
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
	private final Optional<UiAppearanceProvider> appearanceProvider;
	private final LicenseHolder licenseHolder;
	private final UiAppearanceListener systemInterfaceThemeListener = this::systemInterfaceThemeChanged;

	private final ObjectProperty<Image> screenshot = new SimpleObjectProperty<>();

	@Inject
	ChooseExistingVaultController(@AddVaultWizardWindow Stage window, @FxmlScene(FxmlFile.ADDVAULT_WELCOME) Lazy<Scene> welcomeScene, @FxmlScene(FxmlFile.ADDVAULT_SUCCESS) Lazy<Scene> successScene, FxApplicationWindows appWindows, ObjectProperty<Path> vaultPath, @AddVaultWizardWindow ObjectProperty<Vault> vault, VaultListManager vaultListManager, ResourceBundle resourceBundle, Settings settings, Optional<UiAppearanceProvider> appearanceProvider, LicenseHolder licenseHolder) {
		this.window = window;
		this.welcomeScene = welcomeScene;
		this.successScene = successScene;
		this.appWindows = appWindows;
		this.vaultPath = vaultPath;
		this.vault = vault;
		this.vaultListManager = vaultListManager;
		this.resourceBundle = resourceBundle;
		this.settings = settings;
		this.appearanceProvider = appearanceProvider;
		this.licenseHolder = licenseHolder;
	}

	@FXML
	public void initialize() {
		if (SystemUtils.IS_OS_MAC) {
			settings.theme().addListener(this::appThemeChanged);
			setSelectedMacScreenshot(settings.theme().get());
		} else {
			this.screenshot.set(new Image(getClass().getResource("/img/select-masterkey-win.png").toString()));
		}
	}

	private void appThemeChanged(@SuppressWarnings("unused") ObservableValue<? extends UiTheme> observable, @SuppressWarnings("unused") UiTheme oldValue, UiTheme newValue) {
		if (appearanceProvider.isPresent() && oldValue == UiTheme.AUTOMATIC && newValue != UiTheme.AUTOMATIC) {
			try {
				appearanceProvider.get().removeListener(systemInterfaceThemeListener);
			} catch (UiAppearanceException e) {
				LOG.error("Failed to disable automatic theme switching.");
			}
		}
		setSelectedMacScreenshot(newValue);
	}

	private void setSelectedMacScreenshot(UiTheme desiredTheme) {
		UiTheme theme = licenseHolder.isValidLicense() ? desiredTheme : UiTheme.LIGHT;
		switch (theme) {
			case LIGHT -> setLightMacScreenshot();
			case DARK -> setDarkMacScreenshot();
			case AUTOMATIC -> {
				appearanceProvider.ifPresent(provider -> {
					try {
						provider.addListener(systemInterfaceThemeListener);
					} catch (UiAppearanceException e) {
						LOG.error("Failed to enable automatic theme switching.");
					}
				});
				setSystemMacScreenshot();
			}
		}
	}

	private void systemInterfaceThemeChanged(Theme theme) {
		switch (theme) {
			case LIGHT -> setLightMacScreenshot();
			case DARK -> setDarkMacScreenshot();
		}
	}

	private void setSystemMacScreenshot() {
		if (appearanceProvider.isPresent()) {
			systemInterfaceThemeChanged(appearanceProvider.get().getSystemTheme());
		} else {
			LOG.warn("No UiAppearanceProvider present, assuming LIGHT theme...");
			setLightMacScreenshot();
		}
	}

	private void setLightMacScreenshot() {
		this.screenshot.set(new Image(getClass().getResource("/img/select-masterkey-mac.png").toString()));
	}

	private void setDarkMacScreenshot() {
		this.screenshot.set(new Image(getClass().getResource("/img/select-masterkey-mac-dark.png").toString()));
	}

	@FXML
	public void back() {
		window.setScene(welcomeScene.get());
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

	public ObjectProperty<Image> screenshotProperty() {
		return screenshot;
	}

	public Image getScreenshot() {
		return screenshot.get();
	}


}

package org.cryptomator.ui.addvaultwizard;

import dagger.Binds;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.SystemUtils;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.mainwindow.MainWindow;

import javax.inject.Named;
import javax.inject.Provider;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

@Module
public abstract class AddVaultModule {

	@Provides
	@AddVaultWizard
	@AddVaultWizardScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@AddVaultWizard
	@AddVaultWizardScoped
	static Stage provideStage(@MainWindow Stage owner, ResourceBundle resourceBundle, @Named("windowIcon") Optional<Image> windowIcon, @AddVaultWizard Lazy<Map<KeyCodeCombination, Runnable>> accelerators) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("addvaultwizard.title"));
		stage.setResizable(false);
		stage.initStyle(StageStyle.DECORATED);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		stage.sceneProperty().addListener(observable -> {
			stage.getScene().getAccelerators().putAll(accelerators.get());
		});
		windowIcon.ifPresent(stage.getIcons()::add);
		return stage;
	}

	@Provides
	@AddVaultWizardScoped
	static ObjectProperty<Path> provideVaultPath() {
		return new SimpleObjectProperty<>();
	}

	@Provides
	@AddVaultWizardScoped
	static StringProperty provideVaultName() {
		return new SimpleStringProperty("");
	}

	@Provides
	@AddVaultWizard
	@AddVaultWizardScoped
	static ObjectProperty<Vault> provideVault() {
		return new SimpleObjectProperty<>();
	}

	// ------------------

	@Provides
	@AddVaultWizard
	@AddVaultWizardScoped
	static Map<KeyCodeCombination, Runnable> provideDefaultAccellerators(@AddVaultWizard Set<Map.Entry<KeyCombination, Runnable>> accelerators) {
		return Map.ofEntries(accelerators.toArray(Map.Entry[]::new));
	}

	@Provides
	@IntoSet
	@AddVaultWizard
	static Map.Entry<KeyCombination, Runnable> provideCloseWindowShortcut(@AddVaultWizard Stage window) {
		if (SystemUtils.IS_OS_WINDOWS) {
			return Map.entry(new KeyCodeCombination(KeyCode.F4, KeyCombination.ALT_DOWN), window::close);
		} else {
			return Map.entry(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN), window::close);
		}
	}

	// ------------------

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_WELCOME)
	@AddVaultWizardScoped
	static Scene provideWelcomeScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders, @AddVaultWizard Stage window) {
		Scene scene = fxmlLoaders.createScene("/fxml/addvault_welcome.fxml");

		KeyCombination cmdW = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
		scene.getAccelerators().put(cmdW, window::close);

		return scene;
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_EXISTING)
	@AddVaultWizardScoped
	static Scene provideChooseExistingVaultScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders, @AddVaultWizard Stage window) {
		return fxmlLoaders.createScene("/fxml/addvault_existing.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_NAME)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultNameScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders, @AddVaultWizard Stage window) {
		return fxmlLoaders.createScene("/fxml/addvault_new_name.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_LOCATION)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultLocationScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders, @AddVaultWizard Stage window) {
		return fxmlLoaders.createScene("/fxml/addvault_new_location.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_PASSWORD)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultPasswordScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders, @AddVaultWizard Stage window) {
		return fxmlLoaders.createScene("/fxml/addvault_new_password.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_SUCCESS)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultSuccessScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders, @AddVaultWizard Stage window) {
		return fxmlLoaders.createScene("/fxml/addvault_success.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(AddVaultWelcomeController.class)
	abstract FxController bindWelcomeController(AddVaultWelcomeController controller);

	@Binds
	@IntoMap
	@FxControllerKey(ChooseExistingVaultController.class)
	abstract FxController bindChooseExistingVaultController(ChooseExistingVaultController controller);

	@Binds
	@IntoMap
	@FxControllerKey(CreateNewVaultNameController.class)
	abstract FxController bindCreateNewVaultNameController(CreateNewVaultNameController controller);

	@Binds
	@IntoMap
	@FxControllerKey(CreateNewVaultLocationController.class)
	abstract FxController bindCreateNewVaultLocationController(CreateNewVaultLocationController controller);

	@Binds
	@IntoMap
	@FxControllerKey(CreateNewVaultPasswordController.class)
	abstract FxController bindCreateNewVaultPasswordController(CreateNewVaultPasswordController controller);

	@Binds
	@IntoMap
	@FxControllerKey(AddVaultSuccessController.class)
	abstract FxController bindAddVaultSuccessController(AddVaultSuccessController controller);
}

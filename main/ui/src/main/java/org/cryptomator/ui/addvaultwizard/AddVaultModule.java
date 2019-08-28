package org.cryptomator.ui.addvaultwizard;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
	static Stage provideStage(@MainWindow Stage owner, ResourceBundle resourceBundle, @Named("windowIcon") Optional<Image> windowIcon) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("addvaultwizard.title"));
		stage.setResizable(false);
		stage.initStyle(StageStyle.DECORATED);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
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
	@FxmlScene(FxmlFile.ADDVAULT_WELCOME)
	@AddVaultWizardScoped
	static Scene provideWelcomeScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/addvault_welcome.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_EXISTING)
	@AddVaultWizardScoped
	static Scene provideChooseExistingVaultScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/addvault_existing.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_NAME)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultNameScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/addvault_new_name.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_LOCATION)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultLocationScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/addvault_new_location.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.ADDVAULT_NEW_PASSWORD)
	@AddVaultWizardScoped
	static Scene provideCreateNewVaultPasswordScene(@AddVaultWizard FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/addvault_new_password.fxml");
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
}

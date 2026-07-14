package org.cryptomator.ui.importtemplate;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.fxapp.PrimaryStage;

import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class ImportTemplateModule {

	@Provides
	@ImportTemplateWindow
	@ImportTemplateScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@ImportTemplateWindow
	@ImportTemplateScoped
	static Stage provideStage(StageFactory factory, @PrimaryStage Stage primaryStage, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(primaryStage);
		stage.setTitle(resourceBundle.getString("importTemplate.title"));
		return stage;
	}

	@Provides
	@ImportTemplateScoped
	static ObjectProperty<Path> provideVaultPath() {
		return new SimpleObjectProperty<>();
	}

	@Provides
	@ImportTemplateWindow
	@ImportTemplateScoped
	static ObjectProperty<Vault> provideVault() {
		return new SimpleObjectProperty<>();
	}

	// ------------------

	@Provides
	@FxmlScene(FxmlFile.IMPORT_TEMPLATE_LOCATION)
	@ImportTemplateScoped
	static Scene provideImportTemplateLocationScene(@ImportTemplateWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.IMPORT_TEMPLATE_LOCATION);
	}

	@Provides
	@FxmlScene(FxmlFile.IMPORT_TEMPLATE_SUCCESS)
	@ImportTemplateScoped
	static Scene provideImportTemplateSuccessScene(@ImportTemplateWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.IMPORT_TEMPLATE_SUCCESS);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(ImportTemplateLocationController.class)
	abstract FxController bindImportTemplateLocationController(ImportTemplateLocationController controller);

	@Binds
	@IntoMap
	@FxControllerKey(ImportTemplateSuccessController.class)
	abstract FxController bindImportTemplateSuccessController(ImportTemplateSuccessController controller);

}

package org.cryptomator.ui.migration;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.cryptofs.common.FileSystemCapabilityChecker;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.mainwindow.MainWindow;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class MigrationModule {

	@Provides
	@MigrationWindow
	@MigrationScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@MigrationWindow
	@MigrationScoped
	static Stage provideStage(StageFactory factory, @MainWindow Stage owner, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("migration.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		return stage;
	}
	
	@Provides
	@Named("capabilityErrorCause")
	@MigrationScoped
	static ObjectProperty<FileSystemCapabilityChecker.Capability> provideCapabilityErrorCause() {
		return new SimpleObjectProperty<>();
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_START)
	@MigrationScoped
	static Scene provideMigrationStartScene(@MigrationWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/migration_start.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_RUN)
	@MigrationScoped
	static Scene provideMigrationRunScene(@MigrationWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/migration_run.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_SUCCESS)
	@MigrationScoped
	static Scene provideMigrationSuccessScene(@MigrationWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/migration_success.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_CAPABILITY_ERROR)
	@MigrationScoped
	static Scene provideMigrationCapabilityErrorScene(@MigrationWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/migration_capability_error.fxml");
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_IMPOSSIBLE)
	@MigrationScoped
	static Scene provideMigrationImpossibleScene(@MigrationWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/migration_impossible.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(MigrationStartController.class)
	abstract FxController bindMigrationStartController(MigrationStartController controller);

	@Binds
	@IntoMap
	@FxControllerKey(MigrationRunController.class)
	abstract FxController bindMigrationRunController(MigrationRunController controller);

	@Binds
	@IntoMap
	@FxControllerKey(MigrationSuccessController.class)
	abstract FxController bindMigrationSuccessController(MigrationSuccessController controller);

	@Binds
	@IntoMap
	@FxControllerKey(MigrationCapabilityErrorController.class)
	abstract FxController bindMigrationCapabilityErrorController(MigrationCapabilityErrorController controller);

	@Binds
	@IntoMap
	@FxControllerKey(MigrationImpossibleController.class)
	abstract FxController bindMigrationImpossibleController(MigrationImpossibleController controller);
}

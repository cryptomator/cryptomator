package org.cryptomator.ui.migration;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.cryptofs.common.FileSystemCapabilityChecker;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.mainwindow.MainWindow;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class MigrationModule {

	@Provides
	@MigrationWindow
	@MigrationScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
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
	static Scene provideMigrationStartScene(@MigrationWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.MIGRATION_START);
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_RUN)
	@MigrationScoped
	static Scene provideMigrationRunScene(@MigrationWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.MIGRATION_RUN);
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_SUCCESS)
	@MigrationScoped
	static Scene provideMigrationSuccessScene(@MigrationWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.MIGRATION_SUCCESS);
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_CAPABILITY_ERROR)
	@MigrationScoped
	static Scene provideMigrationCapabilityErrorScene(@MigrationWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.MIGRATION_CAPABILITY_ERROR);
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_IMPOSSIBLE)
	@MigrationScoped
	static Scene provideMigrationImpossibleScene(@MigrationWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.MIGRATION_IMPOSSIBLE);
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

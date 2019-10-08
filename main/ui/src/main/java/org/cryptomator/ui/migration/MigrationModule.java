package org.cryptomator.ui.migration;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.mainwindow.MainWindow;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;
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
	static Stage provideStage(@MainWindow Stage owner, ResourceBundle resourceBundle, @Named("windowIcon") Optional<Image> windowIcon) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("migration.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		windowIcon.ifPresent(stage.getIcons()::add);
		return stage;
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

}

package org.cryptomator.ui.migration;

import dagger.Binds;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dagger.multibindings.IntoSet;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.SystemUtils;
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
import java.util.Set;

@Module
abstract class MigrationModule {

	@Provides
	@MigrationWindow
	@MigrationScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}
	
	@Provides
	@MigrationWindow
	@MigrationScoped
	static Map<KeyCodeCombination, Runnable> provideDefaultAccellerators(@MigrationWindow Set<Map.Entry<KeyCombination, Runnable>> accelerators) {
		return Map.ofEntries(accelerators.toArray(Map.Entry[]::new));
	}

	@Provides
	@MigrationWindow
	@MigrationScoped
	static Stage provideStage(@MainWindow Stage owner, ResourceBundle resourceBundle, @Named("windowIcon") Optional<Image> windowIcon, @MigrationWindow Lazy<Map<KeyCodeCombination, Runnable>> accelerators) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("migration.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		stage.sceneProperty().addListener(observable -> {
			stage.getScene().getAccelerators().putAll(accelerators.get());
		});
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

	@Provides
	@IntoSet
	@MigrationWindow
	static Map.Entry<KeyCombination, Runnable> provideCloseWindowShortcut(@MigrationWindow Stage window) {
		if (SystemUtils.IS_OS_WINDOWS) {
			return Map.entry(new KeyCodeCombination(KeyCode.F4, KeyCombination.ALT_DOWN), window::close);
		} else {
			return Map.entry(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN), window::close);
		}
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

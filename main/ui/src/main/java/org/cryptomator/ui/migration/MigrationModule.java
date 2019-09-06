package org.cryptomator.ui.migration;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
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
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@MigrationWindow
	@MigrationScoped
	static Stage provideStage(@MainWindow Stage owner, ResourceBundle resourceBundle, @Named("windowIcon") Optional<Image> windowIcon) {
		Stage stage = new Stage();
		stage.setTitle(resourceBundle.getString("unlock.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		windowIcon.ifPresent(stage.getIcons()::add);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.MIGRATION_START)
	@MigrationScoped
	static Scene provideMigrationStartScene(@MigrationWindow FXMLLoaderFactory fxmlLoaders, @MigrationWindow Stage window) {
		Scene scene = fxmlLoaders.createScene("/fxml/migration_start.fxml"); // TODO rename fxml file

		KeyCombination cmdW = new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN);
		scene.getAccelerators().put(cmdW, window::close);

		return scene;
	}


	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(MigrationStartController.class)
	abstract FxController bindMigrationStartController(MigrationStartController controller);


}

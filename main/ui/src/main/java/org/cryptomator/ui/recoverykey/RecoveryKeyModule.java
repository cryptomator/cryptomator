package org.cryptomator.ui.recoverykey;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;

import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Module
abstract class RecoveryKeyModule {

	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, resourceBundle);
	}

	@Provides
	@RecoveryKeyWindow
	@RecoveryKeyScoped
	static Stage provideStage(ResourceBundle resourceBundle, @Named("windowIcon") Optional<Image> windowIcon, @Named("keyRecoveryOwner") Stage owner) {
		Stage stage = new Stage();
		stage.setTitle("TODO keyRecovery.title"); // TODO localize
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		windowIcon.ifPresent(stage.getIcons()::add);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.RECOVERYKEY_CREATE)
	@RecoveryKeyScoped
	static Scene provideRecoveryKeyCreationScene(@RecoveryKeyWindow FXMLLoaderFactory fxmlLoaders, @RecoveryKeyWindow Stage window) {
		return fxmlLoaders.createScene("/fxml/recoverykey_create.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(RecoveryKeyCreationController.class)
	abstract FxController bindRecoveryKeyCreationController(RecoveryKeyCreationController controller);
}

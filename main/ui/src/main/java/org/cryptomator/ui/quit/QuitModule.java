package org.cryptomator.ui.quit;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FXMLLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class QuitModule {

	@Provides
	@QuitWindow
	@QuitScoped
	static FXMLLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FXMLLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@QuitWindow
	@QuitScoped
	static Stage provideStage(StageFactory factory) {
		Stage stage = factory.create();
		stage.setMinWidth(300);
		stage.setMinHeight(100);
		stage.initModality(Modality.APPLICATION_MODAL);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.QUIT)
	@QuitScoped
	static Scene provideUnlockScene(@QuitWindow FXMLLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene("/fxml/quit.fxml");
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(QuitController.class)
	abstract FxController bindQuitController(QuitController controller);

}

package org.cryptomator.ui.quitforced;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class QuitForcedModule {

	@Provides
	@QuitForcedWindow
	@QuitForcedScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@QuitForcedWindow
	@QuitForcedScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setMinWidth(300);
		stage.setMinHeight(100);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setTitle(resourceBundle.getString("forcedQuit.title"));
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.QUIT_FORCED)
	@QuitForcedScoped
	static Scene provideQuitScene(@QuitForcedWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.QUIT_FORCED);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(QuitForcedController.class)
	abstract FxController bindQuitController(QuitForcedController controller);

}

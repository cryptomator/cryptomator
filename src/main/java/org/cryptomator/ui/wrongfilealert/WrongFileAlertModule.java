package org.cryptomator.ui.wrongfilealert;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;
import org.cryptomator.ui.mainwindow.MainWindow;

import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module
abstract class WrongFileAlertModule {

	@Provides
	@WrongFileAlertWindow
	@WrongFileAlertScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@WrongFileAlertWindow
	@WrongFileAlertScoped
	static Stage provideStage(StageFactory factory, @MainWindow Stage mainWindow, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("wrongFileAlert.title"));
		stage.setResizable(false);
		stage.initOwner(mainWindow);
		stage.initModality(Modality.WINDOW_MODAL);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.WRONGFILEALERT)
	@WrongFileAlertScoped
	static Scene provideWrongFileAlertScene(@WrongFileAlertWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.WRONGFILEALERT);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(WrongFileAlertController.class)
	abstract FxController bindWrongFileAlertController(WrongFileAlertController controller);
}

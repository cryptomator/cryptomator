package org.cryptomator.ui.dokanysupportend;

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
abstract class DokanySupportEndModule {

	@Provides
	@DokanySupportEndWindow
	@DokanySupportEndScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@DokanySupportEndWindow
	@DokanySupportEndScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("dokanySupportEnd.title"));
		stage.setMinWidth(500);
		stage.setMinHeight(100);
		stage.initModality(Modality.APPLICATION_MODAL);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.DOKANY_SUPPORT_END)
	@DokanySupportEndScoped
	static Scene provideDokanySupportEndScene(@DokanySupportEndWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.DOKANY_SUPPORT_END);
	}


	@Binds
	@IntoMap
	@FxControllerKey(DokanySupportEndController.class)
	abstract FxController bindDokanySupportEndController(DokanySupportEndController controller);

}

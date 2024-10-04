package org.cryptomator.ui.removecert;

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
abstract class RemoveCertModule {

	@Provides
	@RemoveCertWindow
	@RemoveCertScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@RemoveCertWindow
	@RemoveCertScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("removeCert.title"));
		stage.setResizable(false);
		stage.initModality(Modality.WINDOW_MODAL);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.REMOVE_CERT)
	@RemoveCertScoped
	static Scene provideRemoveCertScene(@RemoveCertWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.REMOVE_CERT);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(RemoveCertController.class)
	abstract FxController bindRemoveCertController(RemoveCertController controller);
}

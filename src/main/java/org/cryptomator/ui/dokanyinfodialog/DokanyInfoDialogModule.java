package org.cryptomator.ui.dokanyinfodialog;

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
abstract class DokanyInfoDialogModule {

	@Provides
	@DokanyInfoDialogWindow
	@DokanyInfoDialogScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@DokanyInfoDialogWindow
	@DokanyInfoDialogScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setTitle(resourceBundle.getString("dokanyInfo.title"));
		stage.setMinWidth(500);
		stage.setMinHeight(100);
		stage.initModality(Modality.APPLICATION_MODAL);
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.DOKANY_INFO_DIALOG)
	@DokanyInfoDialogScoped
	static Scene provideDokanyInfoDialogScene(@DokanyInfoDialogWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.DOKANY_INFO_DIALOG);
	}


	@Binds
	@IntoMap
	@FxControllerKey(DokanyInfoDialogController.class)
	abstract FxController bindDokanyInfoDialogController(DokanyInfoDialogController controller);

}

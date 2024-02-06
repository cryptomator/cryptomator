package org.cryptomator.ui.sharevault;

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
abstract class ShareVaultModule {

	@Provides
	@ShareVaultWindow
	@ShareVaultScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@ShareVaultWindow
	@ShareVaultScoped
	static Stage provideStage(StageFactory factory, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setResizable(false);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setTitle(resourceBundle.getString("shareVault.title"));
		return stage;
	}

	@Provides
	@FxmlScene(FxmlFile.SHARE_VAULT)
	@ShareVaultScoped
	static Scene provideShareVaultScene(@ShareVaultWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.SHARE_VAULT);
	}

	@Binds
	@IntoMap
	@FxControllerKey(ShareVaultController.class)
	abstract FxController bindShareVaultController(ShareVaultController controller);
}

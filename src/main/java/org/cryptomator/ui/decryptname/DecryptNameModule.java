package org.cryptomator.ui.decryptname;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import org.cryptomator.common.vaults.Vault;
import org.cryptomator.ui.common.DefaultSceneFactory;
import org.cryptomator.ui.common.FxController;
import org.cryptomator.ui.common.FxControllerKey;
import org.cryptomator.ui.common.FxmlFile;
import org.cryptomator.ui.common.FxmlLoaderFactory;
import org.cryptomator.ui.common.FxmlScene;
import org.cryptomator.ui.common.StageFactory;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.ResourceBundle;

@Module
public abstract class DecryptNameModule {

	@Provides
	@DecryptNameScoped
	@DecryptNameWindow
	static Stage provideStage(StageFactory factory, @Named("windowOwner") Stage owner, @DecryptNameWindow Vault vault, ResourceBundle resourceBundle) {
		Stage stage = factory.create();
		stage.setResizable(true);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(owner);
		stage.setTitle("TODO Decrypt Name");
		vault.stateProperty().addListener(((_, _, _) -> stage.close())); //as soon as the state changes from unlocked, close the window
		//stage.setTitle(resourceBundle.getString("convertVault.title"));
		return stage;
	}

	@Provides
	@DecryptNameScoped
	@DecryptNameWindow
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@FxmlScene(FxmlFile.DECRYPTNAMES)
	@DecryptNameScoped
	static Scene provideDecryptNamesViewScene(@DecryptNameWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.DECRYPTNAMES);
	}

	@Binds
	@IntoMap
	@FxControllerKey(DecryptFileNamesViewController.class)
	abstract FxController bindDecryptNamesViewController(DecryptFileNamesViewController controller);

	@Binds
	@IntoMap
	@FxControllerKey(CipherAndCleartextCellController.class)
	abstract FxController binCipherAndCleartextCellController(CipherAndCleartextCellController controller);
}

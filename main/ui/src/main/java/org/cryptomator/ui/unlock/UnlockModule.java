package org.cryptomator.ui.unlock;

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
import org.cryptomator.ui.keyloading.KeyLoadingComponent;
import org.cryptomator.ui.keyloading.KeyLoadingStrategy;

import javax.inject.Named;
import javax.inject.Provider;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Module(subcomponents = {KeyLoadingComponent.class})
abstract class UnlockModule {

	@Provides
	@UnlockWindow
	@UnlockScoped
	static FxmlLoaderFactory provideFxmlLoaderFactory(Map<Class<? extends FxController>, Provider<FxController>> factories, DefaultSceneFactory sceneFactory, ResourceBundle resourceBundle) {
		return new FxmlLoaderFactory(factories, sceneFactory, resourceBundle);
	}

	@Provides
	@UnlockWindow
	@UnlockScoped
	static Stage provideStage(StageFactory factory, @UnlockWindow Vault vault, @Named("unlockWindowOwner") Optional<Stage> owner) {
		Stage stage = factory.create();
		stage.setTitle(vault.getDisplayName());
		stage.setResizable(false);
		if (owner.isPresent()) {
			stage.initOwner(owner.get());
			stage.initModality(Modality.WINDOW_MODAL);
		} else {
			stage.initModality(Modality.APPLICATION_MODAL);
		}
		return stage;
	}

	@Provides
	@UnlockWindow
	@UnlockScoped
	static KeyLoadingStrategy provideKeyLoadingStrategy(KeyLoadingComponent.Builder compBuilder, @UnlockWindow Vault vault, @UnlockWindow Stage window) {
		return compBuilder.vault(vault).window(window).build().keyloadingStrategy();
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_SUCCESS)
	@UnlockScoped
	static Scene provideUnlockSuccessScene(@UnlockWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.UNLOCK_SUCCESS);
	}

	@Provides
	@FxmlScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT)
	@UnlockScoped
	static Scene provideInvalidMountPointScene(@UnlockWindow FxmlLoaderFactory fxmlLoaders) {
		return fxmlLoaders.createScene(FxmlFile.UNLOCK_INVALID_MOUNT_POINT);
	}

	// ------------------

	@Binds
	@IntoMap
	@FxControllerKey(UnlockSuccessController.class)
	abstract FxController bindUnlockSuccessController(UnlockSuccessController controller);

	@Binds
	@IntoMap
	@FxControllerKey(UnlockInvalidMountPointController.class)
	abstract FxController bindUnlockInvalidMountPointController(UnlockInvalidMountPointController controller);

}
